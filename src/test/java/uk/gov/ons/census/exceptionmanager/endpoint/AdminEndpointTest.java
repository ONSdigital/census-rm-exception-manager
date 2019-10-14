package uk.gov.ons.census.exceptionmanager.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.census.exceptionmanager.model.BadMessageReport;
import uk.gov.ons.census.exceptionmanager.model.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.persistence.InMemoryDatabase;

public class AdminEndpointTest {

  @Test
  public void testGetBadMessages() {
    // Given
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    Set testSet = Collections.emptySet();
    when(inMemoryDatabase.getSeenMessageHashes()).thenReturn(testSet);
    AdminEndpoint underTest = new AdminEndpoint(inMemoryDatabase, 500);

    // When
    ResponseEntity<Set<String>> actualResponse = underTest.getBadMessages();

    // Then
    assertThat(actualResponse.getBody()).isEqualTo(testSet);
    verify(inMemoryDatabase).getSeenMessageHashes();
  }

  @Test
  public void testGetBadMessageDetails() {
    // Given
    String testMessageHash = "test message hash";
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    List testList = Collections.emptyList();
    when(inMemoryDatabase.getBadMessageReports(anyString())).thenReturn(testList);
    AdminEndpoint underTest = new AdminEndpoint(inMemoryDatabase, 500);

    // When
    ResponseEntity<List<BadMessageReport>> actualResponse =
        underTest.getBadMessageDetails(testMessageHash);

    // Then
    assertThat(actualResponse.getBody()).isEqualTo(testList);
    verify(inMemoryDatabase).getBadMessageReports(eq(testMessageHash));
  }

  @Test
  public void testSkipMessage() {
    // Given
    String testMessageHash = "test message hash";
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    AdminEndpoint underTest = new AdminEndpoint(inMemoryDatabase, 500);

    // When
    underTest.skipMessage(testMessageHash);

    // Then
    verify(inMemoryDatabase).skipMessage(eq(testMessageHash));
  }

  @Test
  public void testPeekMessage() {
    // Given
    String testMessageHash = "test message hash";
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    byte[] testPeekedMessageBody = "test message body".getBytes();
    when(inMemoryDatabase.getPeekedMessage(anyString()))
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(testPeekedMessageBody);
    AdminEndpoint underTest = new AdminEndpoint(inMemoryDatabase, 500);

    // When
    ResponseEntity<String> actualResponse = underTest.peekMessage(testMessageHash);

    // Then
    assertThat(actualResponse.getBody()).isEqualTo(new String(testPeekedMessageBody));
    verify(inMemoryDatabase).peekMessage(eq(testMessageHash));
  }

  @Test
  public void testGetAllSkippedMessages() {
    // Given
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    Map testMap = Collections.emptyMap();
    when(inMemoryDatabase.getAllSkippedMessages()).thenReturn(testMap);
    AdminEndpoint underTest = new AdminEndpoint(inMemoryDatabase, 500);

    // When
    ResponseEntity<Map<String, List<SkippedMessage>>> actualResponse =
        underTest.getAllSkippedMessages();

    // Then
    assertThat(actualResponse.getBody()).isEqualTo(testMap);
    verify(inMemoryDatabase).getAllSkippedMessages();
  }

  @Test
  public void testGetSkippedMessage() {
    // Given
    String testMessageHash = "test message hash";
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    List testList = Collections.emptyList();
    when(inMemoryDatabase.getSkippedMessages(anyString())).thenReturn(testList);
    AdminEndpoint underTest = new AdminEndpoint(inMemoryDatabase, 500);

    // When
    ResponseEntity<List<SkippedMessage>> actualResponse =
        underTest.getSkippedMessage(testMessageHash);

    // Then
    assertThat(actualResponse.getBody()).isEqualTo(testList);
    verify(inMemoryDatabase).getSkippedMessages(eq(testMessageHash));
  }

  @Test
  public void testReset() {
    // Given
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    AdminEndpoint underTest = new AdminEndpoint(inMemoryDatabase, 500);

    // When
    underTest.reset();

    // Then
    verify(inMemoryDatabase).reset();
  }
}
