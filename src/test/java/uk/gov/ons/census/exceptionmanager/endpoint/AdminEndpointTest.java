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
import uk.gov.ons.census.exceptionmanager.model.dto.BadMessageReport;
import uk.gov.ons.census.exceptionmanager.model.dto.BadMessageSummary;
import uk.gov.ons.census.exceptionmanager.model.dto.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.dto.ExceptionStats;
import uk.gov.ons.census.exceptionmanager.model.dto.SkippedMessage;
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
  public void getBadMessagesSummary() {
    // Given
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    Set testSet = Set.of("test message hash");
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setQueue("test queue");
    exceptionReport.setService("test service");
    ExceptionStats exceptionStats = new ExceptionStats();
    exceptionStats.getSeenCount().set(666);
    BadMessageReport badMessageReport = new BadMessageReport();
    badMessageReport.setExceptionReport(exceptionReport);
    badMessageReport.setStats(exceptionStats);
    List<BadMessageReport> badMessageReportList = List.of(badMessageReport);
    when(inMemoryDatabase.getSeenMessageHashes()).thenReturn(testSet);
    when(inMemoryDatabase.getBadMessageReports(anyString())).thenReturn(badMessageReportList);
    when(inMemoryDatabase.shouldWeSkipThisMessage(anyString())).thenReturn(true);
    AdminEndpoint underTest = new AdminEndpoint(inMemoryDatabase, 500);

    // When
    ResponseEntity<List<BadMessageSummary>> actualResponse = underTest.getBadMessagesSummary();

    // Then
    verify(inMemoryDatabase).getSeenMessageHashes();
    verify(inMemoryDatabase).getBadMessageReports(eq("test message hash"));
    verify(inMemoryDatabase).shouldWeSkipThisMessage(eq("test message hash"));

    assertThat(actualResponse.getBody()).isNotNull();
    assertThat(actualResponse.getBody().size()).isEqualTo(1);
    BadMessageSummary actualBadMessageSummary = actualResponse.getBody().get(0);
    assertThat(actualBadMessageSummary.getAffectedQueues()).containsOnly("test queue");
    assertThat(actualBadMessageSummary.getAffectedServices()).containsOnly("test service");
    assertThat(actualBadMessageSummary.getFirstSeen()).isEqualTo(exceptionStats.getFirstSeen());
    assertThat(actualBadMessageSummary.getLastSeen()).isEqualTo(exceptionStats.getLastSeen());
    assertThat(actualBadMessageSummary.getSeenCount()).isEqualTo(666);
    assertThat(actualBadMessageSummary.isQuarantined()).isTrue();
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
