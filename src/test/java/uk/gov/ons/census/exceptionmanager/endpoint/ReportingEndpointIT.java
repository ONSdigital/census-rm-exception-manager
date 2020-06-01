package uk.gov.ons.census.exceptionmanager.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.ons.census.exceptionmanager.model.dto.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.dto.Peek;
import uk.gov.ons.census.exceptionmanager.model.dto.Response;
import uk.gov.ons.census.exceptionmanager.model.dto.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.model.entity.QuarantinedMessage;
import uk.gov.ons.census.exceptionmanager.model.repository.QuarantinedMessageRepository;
import uk.gov.ons.census.exceptionmanager.persistence.InMemoryDatabase;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ReportingEndpointIT {
  private static final String TEST_MESSAGE_HASH =
      "9af5350f1e61149cd0bb7dfa5efae46f224aaaffed729b220d63e0fe5a8bf4b9";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @Autowired private InMemoryDatabase inMemoryDatabase;
  @Autowired private QuarantinedMessageRepository quarantinedMessageRepository;

  @LocalServerPort private int port;

  @Before
  public void setUp() {
    quarantinedMessageRepository.deleteAllInBatch();
    inMemoryDatabase.reset();
  }

  @Test
  public void testReportException() throws Exception {
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash(TEST_MESSAGE_HASH);
    exceptionReport.setService("test service");
    exceptionReport.setQueue("test queue");
    exceptionReport.setExceptionClass("test class");
    exceptionReport.setExceptionMessage("test message");

    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/json");
    headers.put("Content-Type", "application/json");
    HttpResponse<String> response =
        Unirest.post(String.format("http://localhost:%d/reportexception", port))
            .headers(headers)
            .body(objectMapper.writeValueAsString(exceptionReport))
            .asString();

    assertThat(response.getStatus()).isEqualTo(OK.value());

    Response actualResponse = objectMapper.readValue(response.getBody(), Response.class);
    assertThat(actualResponse.isSkipIt()).isFalse();
    assertThat(actualResponse.isLogIt()).isTrue();
    assertThat(actualResponse.isPeek()).isFalse();

    assertThat(inMemoryDatabase.getBadMessageReports(TEST_MESSAGE_HASH).size()).isEqualTo(1);
  }

  @Test
  public void testPeekReply() throws Exception {
    Peek peek = new Peek();
    peek.setMessageHash(TEST_MESSAGE_HASH);
    peek.setMessagePayload("test payload".getBytes());

    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/json");
    headers.put("Content-Type", "application/json");
    HttpResponse<String> response =
        Unirest.post(String.format("http://localhost:%d/peekreply", port))
            .headers(headers)
            .body(objectMapper.writeValueAsString(peek))
            .asString();

    assertThat(response.getStatus()).isEqualTo(OK.value());

    assertThat(inMemoryDatabase.getPeekedMessage(TEST_MESSAGE_HASH))
        .isEqualTo(peek.getMessagePayload());
  }

  @Test
  public void testStoreSkippedMessage() throws Exception {
    testReportException();

    SkippedMessage skippedMessage = new SkippedMessage();
    skippedMessage.setMessageHash(TEST_MESSAGE_HASH);
    skippedMessage.setQueue("test queue");
    skippedMessage.setContentType("application/xml");
    skippedMessage.setHeaders(Map.of("foo", "bar"));
    skippedMessage.setMessagePayload("<noodle>poodle</noodle>".getBytes());
    skippedMessage.setRoutingKey("test routing key");
    skippedMessage.setService("test service");
    skippedMessage.setSkippedTimestamp(null);

    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/json");
    headers.put("Content-Type", "application/json");
    HttpResponse<String> response =
        Unirest.post(String.format("http://localhost:%d/storeskippedmessage", port))
            .headers(headers)
            .body(objectMapper.writeValueAsString(skippedMessage))
            .asString();

    assertThat(response.getStatus()).isEqualTo(OK.value());

    List<SkippedMessage> skippedMessages = inMemoryDatabase.getSkippedMessages(TEST_MESSAGE_HASH);
    assertThat(skippedMessages.size()).isEqualTo(1);
    assertThat(skippedMessages.get(0)).isEqualTo(skippedMessage);

    List<QuarantinedMessage> allQuarantinedMessages = quarantinedMessageRepository.findAll();
    assertThat(allQuarantinedMessages.size()).isEqualTo(1);
    QuarantinedMessage quarantinedMessage = allQuarantinedMessages.get(0);
    assertThat(quarantinedMessage.getContentType()).isEqualTo(skippedMessage.getContentType());
    assertThat(quarantinedMessage.getHeaders()).isEqualTo(skippedMessage.getHeaders());
    assertThat(quarantinedMessage.getMessagePayload())
        .isEqualTo(skippedMessage.getMessagePayload());
    assertThat(quarantinedMessage.getRoutingKey()).isEqualTo(skippedMessage.getRoutingKey());
    assertThat(quarantinedMessage.getService()).isEqualTo(skippedMessage.getService());
    assertThat(quarantinedMessage.getSkippedTimestamp()).isNotNull();
    assertThat(quarantinedMessage.getErrorReports()).isNotBlank();
  }
}
