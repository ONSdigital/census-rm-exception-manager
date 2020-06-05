package uk.gov.ons.census.exceptionmanager.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.census.exceptionmanager.model.dto.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.dto.Peek;
import uk.gov.ons.census.exceptionmanager.model.dto.Response;
import uk.gov.ons.census.exceptionmanager.model.dto.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.model.entity.QuarantinedMessage;
import uk.gov.ons.census.exceptionmanager.model.repository.QuarantinedMessageRepository;
import uk.gov.ons.census.exceptionmanager.persistence.InMemoryDatabase;

public class ReportingEndpointTest {

  @Test
  public void testReportError() {
    String testMessageHash = "test message hash";
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    ReportingEndpoint underTest = new ReportingEndpoint(inMemoryDatabase, null);
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash(testMessageHash);

    when(inMemoryDatabase.shouldWeSkipThisMessage(any(ExceptionReport.class))).thenReturn(true);
    when(inMemoryDatabase.shouldWePeekThisMessage(anyString())).thenReturn(true);
    when(inMemoryDatabase.shouldWeLogThisMessage(exceptionReport)).thenReturn(true);

    ResponseEntity<Response> actualResponse = underTest.reportError(exceptionReport);

    verify(inMemoryDatabase).shouldWeSkipThisMessage(eq(exceptionReport));
    verify(inMemoryDatabase).shouldWePeekThisMessage(eq(testMessageHash));
    verify(inMemoryDatabase).shouldWeLogThisMessage(eq(exceptionReport));
    assertThat(actualResponse.getBody().isSkipIt()).isTrue();
    assertThat(actualResponse.getBody().isPeek()).isTrue();
    assertThat(actualResponse.getBody().isLogIt()).isTrue();
  }

  @Test
  public void testPeekReply() {
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    ReportingEndpoint underTest = new ReportingEndpoint(inMemoryDatabase, null);
    Peek peek = new Peek();

    underTest.peekReply(peek);

    verify(inMemoryDatabase).storePeekMessageReply(eq(peek));
  }

  @Test
  public void testStoreSkippedMessage() {
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    QuarantinedMessageRepository quarantinedMessageRepository =
        mock(QuarantinedMessageRepository.class);
    ReportingEndpoint underTest =
        new ReportingEndpoint(inMemoryDatabase, quarantinedMessageRepository);
    SkippedMessage skippedMessage = new SkippedMessage();
    skippedMessage.setMessageHash("test message hash");
    skippedMessage.setQueue("test queue");
    skippedMessage.setContentType("application/xml");
    skippedMessage.setHeaders(Map.of("foo", "bar"));
    skippedMessage.setMessagePayload("<noodle>poodle</noodle>".getBytes());
    skippedMessage.setRoutingKey("test routing key");
    skippedMessage.setService("test service");

    underTest.storeSkippedMessage(skippedMessage);

    verify(inMemoryDatabase).storeSkippedMessage(eq(skippedMessage));

    ArgumentCaptor<QuarantinedMessage> quarantinedMessageArgCaptor =
        ArgumentCaptor.forClass(QuarantinedMessage.class);
    verify(quarantinedMessageRepository).save(quarantinedMessageArgCaptor.capture());
    QuarantinedMessage quarantinedMessage = quarantinedMessageArgCaptor.getValue();
    assertThat(quarantinedMessage.getContentType()).isEqualTo(skippedMessage.getContentType());
    assertThat(quarantinedMessage.getHeaders()).isEqualTo(skippedMessage.getHeaders());
    assertThat(quarantinedMessage.getMessagePayload())
        .isEqualTo(skippedMessage.getMessagePayload());
    assertThat(quarantinedMessage.getRoutingKey()).isEqualTo(skippedMessage.getRoutingKey());
    assertThat(quarantinedMessage.getService()).isEqualTo(skippedMessage.getService());
  }
}
