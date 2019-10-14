package uk.gov.ons.census.exceptionmanager.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class AdminEndpointIT {
  private static final String TEST_MESSAGE_HASH =
      "9af5350f1e61149cd0bb7dfa5efae46f224aaaffed729b220d63e0fe5a8bf4b9";
  private static final ObjectMapper objectMapper = new ObjectMapper();

  @LocalServerPort private int port;

  @Before
  public void setUp() {}

  @Test
  public void testGetBadMessages() throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/json");
    headers.put("Content-Type", "application/json");
    HttpResponse<String> response =
        Unirest.get(String.format("http://localhost:%d/badmessages", port))
            .headers(headers)
            .asString();

    assertThat(response.getStatus()).isEqualTo(OK.value());

    Set actualResponse = objectMapper.readValue(response.getBody(), Set.class);
    assertThat(actualResponse.size()).isEqualTo(0);
  }

  @Test
  public void testGetBadMessage() throws Exception {
    Map<String, String> headers = new HashMap<>();
    headers.put("accept", "application/json");
    headers.put("Content-Type", "application/json");
    HttpResponse<String> response =
        Unirest.get(String.format("http://localhost:%d/badmessage/%s", port, TEST_MESSAGE_HASH))
            .headers(headers)
            .asString();

    assertThat(response.getStatus()).isEqualTo(OK.value());

    List actualResponse = objectMapper.readValue(response.getBody(), List.class);
    assertThat(actualResponse.size()).isEqualTo(0);
  }
}
