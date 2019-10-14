package uk.gov.ons.census.exceptionmanager.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.springframework.http.ResponseEntity;
import uk.gov.ons.census.exceptionmanager.model.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.Peek;
import uk.gov.ons.census.exceptionmanager.model.Response;
import uk.gov.ons.census.exceptionmanager.model.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.persistence.InMemoryDatabase;

public class ReportingEndpointTest {

  @Test
  public void testReportError() {
    String testMessageHash = "test message hash";
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    ReportingEndpoint underTest = new ReportingEndpoint(inMemoryDatabase);
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash(testMessageHash);

    when(inMemoryDatabase.shouldWeSkipThisMessage(anyString())).thenReturn(true);
    when(inMemoryDatabase.shouldWePeekThisMessage(anyString())).thenReturn(true);
    when(inMemoryDatabase.shouldWeLogThisMessage(exceptionReport)).thenReturn(true);

    ResponseEntity<Response> actualResponse = underTest.reportError(exceptionReport);

    verify(inMemoryDatabase).shouldWeSkipThisMessage(eq(testMessageHash));
    verify(inMemoryDatabase).shouldWePeekThisMessage(eq(testMessageHash));
    verify(inMemoryDatabase).shouldWeLogThisMessage(eq(exceptionReport));
    assertThat(actualResponse.getBody().isSkipIt()).isTrue();
    assertThat(actualResponse.getBody().isPeek()).isTrue();
    assertThat(actualResponse.getBody().isLogIt()).isTrue();
  }

  @Test
  public void testPeekReply() {
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    ReportingEndpoint underTest = new ReportingEndpoint(inMemoryDatabase);
    Peek peek = new Peek();

    underTest.peekReply(peek);

    verify(inMemoryDatabase).storePeekMessageReply(eq(peek));
  }

  @Test
  public void testStoreSkippedMessage() {
    InMemoryDatabase inMemoryDatabase = mock(InMemoryDatabase.class);
    ReportingEndpoint underTest = new ReportingEndpoint(inMemoryDatabase);
    SkippedMessage skippedMessage = new SkippedMessage();

    underTest.storeSkippedMessage(skippedMessage);

    verify(inMemoryDatabase).storeSkippedMessage(eq(skippedMessage));
  }
}
