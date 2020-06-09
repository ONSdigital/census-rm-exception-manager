package uk.gov.ons.census.exceptionmanager.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import uk.gov.ons.census.exceptionmanager.model.dto.BadMessageReport;
import uk.gov.ons.census.exceptionmanager.model.dto.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.dto.ExceptionStats;
import uk.gov.ons.census.exceptionmanager.model.dto.Peek;
import uk.gov.ons.census.exceptionmanager.model.dto.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.model.entity.AutoQuarantineRule;
import uk.gov.ons.census.exceptionmanager.model.repository.AutoQuarantineRuleRepository;

public class CachingDataStoreTest {
  @Test
  public void testUpdateStats() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash("test message hash");
    exceptionReport.setExceptionClass("test class");
    exceptionReport.setExceptionMessage("test exception message");
    exceptionReport.setQueue("test queue");
    exceptionReport.setService("test service");

    underTest.updateStats(exceptionReport);

    assertThat(underTest.shouldWeSkipThisMessage(exceptionReport)).isFalse();
    assertThat(underTest.shouldWeLogThisMessage(exceptionReport)).isFalse();
    assertThat(underTest.shouldWePeekThisMessage("test message hash")).isFalse();

    List<BadMessageReport> badMessageReports = underTest.getBadMessageReports("test message hash");

    assertThat(badMessageReports.size()).isEqualTo(1);
    assertThat(badMessageReports.get(0).getExceptionReport()).isEqualTo(exceptionReport);
    assertThat(badMessageReports.get(0).getStats().getSeenCount().get()).isEqualTo(1);
  }

  @Test
  public void testUpdateStatsSameTwice() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash("test message hash");
    exceptionReport.setExceptionClass("test class");
    exceptionReport.setExceptionMessage("test exception message");
    exceptionReport.setQueue("test queue");
    exceptionReport.setService("test service");

    underTest.updateStats(exceptionReport);
    underTest.updateStats(exceptionReport);

    assertThat(underTest.shouldWeSkipThisMessage(exceptionReport)).isFalse();
    assertThat(underTest.shouldWeLogThisMessage(exceptionReport)).isFalse();
    assertThat(underTest.shouldWePeekThisMessage("test message hash")).isFalse();

    List<BadMessageReport> badMessageReports = underTest.getBadMessageReports("test message hash");

    assertThat(badMessageReports.size()).isEqualTo(1);
    assertThat(badMessageReports.get(0).getExceptionReport()).isEqualTo(exceptionReport);
    assertThat(badMessageReports.get(0).getStats().getSeenCount().get()).isEqualTo(2);
  }

  @Test
  public void testUpdateStatsDifferentTimes() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    ExceptionReport exceptionReportOne = new ExceptionReport();
    exceptionReportOne.setMessageHash("test message hash");
    exceptionReportOne.setExceptionClass("test class");
    exceptionReportOne.setExceptionMessage("test exception message");
    exceptionReportOne.setQueue("test queue");
    exceptionReportOne.setService("test service");

    underTest.updateStats(exceptionReportOne);

    ExceptionReport exceptionReportTwo = new ExceptionReport();
    exceptionReportTwo.setMessageHash("test message hash");
    exceptionReportTwo.setExceptionClass("test class");
    exceptionReportTwo.setExceptionMessage("different test exception message");
    exceptionReportTwo.setQueue("test queue");
    exceptionReportTwo.setService("test service");
    underTest.updateStats(exceptionReportTwo);
    underTest.updateStats(exceptionReportTwo);
    underTest.updateStats(exceptionReportTwo);

    assertThat(underTest.shouldWeSkipThisMessage(exceptionReportOne)).isFalse();
    assertThat(underTest.shouldWeLogThisMessage(exceptionReportOne)).isFalse();
    assertThat(underTest.shouldWeLogThisMessage(exceptionReportTwo)).isFalse();
    assertThat(underTest.shouldWePeekThisMessage("test message hash")).isFalse();

    List<BadMessageReport> badMessageReports = underTest.getBadMessageReports("test message hash");

    assertThat(badMessageReports.size()).isEqualTo(2);
    assertThat(badMessageReports)
        .extracting(BadMessageReport::getExceptionReport)
        .contains(exceptionReportOne, exceptionReportTwo);
    assertThat(badMessageReports)
        .extracting(BadMessageReport::getStats)
        .extracting(ExceptionStats::getSeenCount)
        .extracting(AtomicInteger::get)
        .contains(1, 3);
  }

  @Test
  public void testShouldWeLog() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash("test message hash");
    exceptionReport.setExceptionClass("test class");
    exceptionReport.setExceptionMessage("test exception message");
    exceptionReport.setQueue("test queue");
    exceptionReport.setService("test service");

    assertThat(underTest.shouldWeLogThisMessage(exceptionReport)).isTrue();

    underTest.updateStats(exceptionReport);

    assertThat(underTest.shouldWeLogThisMessage(exceptionReport)).isFalse();
  }

  @Test
  public void testShouldWeSkip() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash("test message hash");
    exceptionReport.setExceptionClass("test class");
    exceptionReport.setExceptionMessage("test exception message");
    exceptionReport.setQueue("test queue");
    exceptionReport.setService("test service");

    assertThat(underTest.shouldWeSkipThisMessage(exceptionReport)).isFalse();

    underTest.updateStats(exceptionReport);

    assertThat(underTest.shouldWeSkipThisMessage(exceptionReport)).isFalse();

    underTest.skipMessage("test message hash");

    assertThat(underTest.shouldWeSkipThisMessage(exceptionReport)).isTrue();
  }

  @Test
  public void testShouldWeSkipAutoQuarantineMatch() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    AutoQuarantineRule rule = new AutoQuarantineRule();
    rule.setExpression("exceptionClass == \"test class\" and queue == \"test queue\"");
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.singletonList(rule));
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash("test message hash");
    exceptionReport.setExceptionClass("test class");
    exceptionReport.setExceptionMessage("test exception message");
    exceptionReport.setQueue("test queue");
    exceptionReport.setService("test service");

    assertThat(underTest.shouldWeSkipThisMessage(exceptionReport)).isTrue();
  }

  @Test
  public void testShouldWeSkipAutoQuarantineMatchSubstring() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    AutoQuarantineRule rule = new AutoQuarantineRule();
    rule.setExpression("exceptionMessage.contains('message')");
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.singletonList(rule));
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash("test message hash");
    exceptionReport.setExceptionClass("test class");
    exceptionReport.setExceptionMessage("test exception message");
    exceptionReport.setQueue("test queue");
    exceptionReport.setService("test service");

    assertThat(underTest.shouldWeSkipThisMessage(exceptionReport)).isTrue();
  }

  @Test
  public void testShouldWeSkipAutoQuarantineDoesNotMatch() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    AutoQuarantineRule rule = new AutoQuarantineRule();
    rule.setExpression("exceptionClass == \"noodle\" and queue == \"test queue\"");
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.singletonList(rule));
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash("test message hash");
    exceptionReport.setExceptionClass("test class");
    exceptionReport.setExceptionMessage("test exception message");
    exceptionReport.setQueue("test queue");
    exceptionReport.setService("test service");

    assertThat(underTest.shouldWeSkipThisMessage(exceptionReport)).isFalse();
  }

  @Test
  public void testShouldWePeek() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    ExceptionReport exceptionReport = new ExceptionReport();
    exceptionReport.setMessageHash("test message hash");
    exceptionReport.setExceptionClass("test class");
    exceptionReport.setExceptionMessage("test exception message");
    exceptionReport.setQueue("test queue");
    exceptionReport.setService("test service");

    assertThat(underTest.shouldWePeekThisMessage("test message hash")).isFalse();

    underTest.updateStats(exceptionReport);

    assertThat(underTest.shouldWePeekThisMessage("test message hash")).isFalse();

    underTest.peekMessage("test message hash");

    assertThat(underTest.shouldWePeekThisMessage("test message hash")).isTrue();
  }

  @Test
  public void testGetSeenMessageHashes() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    ExceptionReport exceptionReportOne = new ExceptionReport();
    exceptionReportOne.setMessageHash("test message hash");
    exceptionReportOne.setExceptionClass("test class");
    exceptionReportOne.setExceptionMessage("test exception message");
    exceptionReportOne.setQueue("test queue");
    exceptionReportOne.setService("test service");

    underTest.updateStats(exceptionReportOne);

    ExceptionReport exceptionReportTwo = new ExceptionReport();
    exceptionReportTwo.setMessageHash("another test message hash");
    exceptionReportTwo.setExceptionClass("test class");
    exceptionReportTwo.setExceptionMessage("test exception message");
    exceptionReportTwo.setQueue("test queue");
    exceptionReportTwo.setService("test service");
    underTest.updateStats(exceptionReportTwo);
    underTest.updateStats(exceptionReportTwo);
    underTest.updateStats(exceptionReportTwo);

    underTest.updateStats(exceptionReportTwo);

    assertThat(underTest.getSeenMessageHashes())
        .contains("test message hash", "another test message hash");
  }

  @Test
  public void testStorePeekMessageReply() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    Peek peek = new Peek();
    peek.setMessageHash("test message hash");
    peek.setMessagePayload("test message".getBytes());
    underTest.storePeekMessageReply(peek);

    assertThat(underTest.getPeekedMessage("test message hash"))
        .isEqualTo("test message".getBytes());
  }

  @Test
  public void testStoreSkippedMessage() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    SkippedMessage skippedMessage = new SkippedMessage();
    skippedMessage.setMessageHash("test message hash");
    underTest.storeSkippedMessage(skippedMessage);

    assertThat(underTest.getSkippedMessages("test message hash")).contains(skippedMessage);
    assertThat(underTest.getAllSkippedMessages())
        .containsOnlyKeys("test message hash")
        .containsValue(List.of(skippedMessage));
  }

  @Test
  public void testStoreTwoSkippedMessagse() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    SkippedMessage skippedMessageOne = new SkippedMessage();
    skippedMessageOne.setMessageHash("test message hash");
    skippedMessageOne.setQueue("test queue one");
    underTest.storeSkippedMessage(skippedMessageOne);

    SkippedMessage skippedMessageTwo = new SkippedMessage();
    skippedMessageTwo.setMessageHash("test message hash");
    skippedMessageTwo.setQueue("test queue twp");
    underTest.storeSkippedMessage(skippedMessageTwo);

    assertThat(underTest.getSkippedMessages("test message hash"))
        .contains(skippedMessageOne, skippedMessageTwo);
  }

  @Test
  public void testReset() {
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(Collections.emptyList());
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    SkippedMessage skippedMessageOne = new SkippedMessage();
    skippedMessageOne.setMessageHash("test message hash");
    skippedMessageOne.setQueue("test queue one");
    underTest.storeSkippedMessage(skippedMessageOne);

    SkippedMessage skippedMessageTwo = new SkippedMessage();
    skippedMessageTwo.setMessageHash("test message hash");
    skippedMessageTwo.setQueue("test queue twp");
    underTest.storeSkippedMessage(skippedMessageTwo);

    Peek peek = new Peek();
    peek.setMessageHash("test message hash");
    peek.setMessagePayload("test message".getBytes());
    underTest.storePeekMessageReply(peek);

    ExceptionReport exceptionReportOne = new ExceptionReport();
    exceptionReportOne.setMessageHash("test message hash");
    exceptionReportOne.setExceptionClass("test class");
    exceptionReportOne.setExceptionMessage("test exception message");
    exceptionReportOne.setQueue("test queue");
    exceptionReportOne.setService("test service");

    underTest.updateStats(exceptionReportOne);

    ExceptionReport exceptionReportTwo = new ExceptionReport();
    exceptionReportTwo.setMessageHash("another test message hash");
    exceptionReportTwo.setExceptionClass("test class");
    exceptionReportTwo.setExceptionMessage("test exception message");
    exceptionReportTwo.setQueue("test queue");
    exceptionReportTwo.setService("test service");
    underTest.updateStats(exceptionReportTwo);
    underTest.updateStats(exceptionReportTwo);
    underTest.updateStats(exceptionReportTwo);
    underTest.updateStats(exceptionReportTwo);

    SkippedMessage skippedMessage = new SkippedMessage();
    skippedMessage.setMessageHash("test message hash");
    underTest.storeSkippedMessage(skippedMessage);

    assertThat(underTest.getSkippedMessages("test message hash")).contains(skippedMessage);
    assertThat(
        underTest
            .getAllSkippedMessages()
            .get("test message hash")
            .contains(List.of(skippedMessage)));

    underTest.reset();

    assertThat(underTest.getSkippedMessages("test message hash")).contains(skippedMessage);
    assertThat(
        underTest
            .getAllSkippedMessages()
            .get("test message hash")
            .contains(List.of(skippedMessage)));

    assertThat(underTest.getAllSkippedMessages()).isNotEmpty();
    assertThat(underTest.getPeekedMessage("test message hash")).isNullOrEmpty();
    assertThat(underTest.getBadMessageReports("test message hash")).isEmpty();
    assertThat(underTest.getBadMessageReports("test message hash")).isEmpty();
    assertThat(underTest.getAllSkippedMessages()).isNotEmpty();
    assertThat(underTest.getAllSkippedMessages()).isNotEmpty();
  }

  @Test
  public void testGetQuarantineRules() {
    // Given
    List<AutoQuarantineRule> expectedAutoQuarantineRules = Collections.EMPTY_LIST;
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(expectedAutoQuarantineRules);
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);

    // When
    List<AutoQuarantineRule> actualQuarantineRules = underTest.getQuarantineRules();

    // Then
    assertThat(actualQuarantineRules).isEqualTo(expectedAutoQuarantineRules);
  }

  @Test
  public void testDeleteQuarantineRule() {
    List<AutoQuarantineRule> expectedAutoQuarantineRules = Collections.EMPTY_LIST;
    AutoQuarantineRuleRepository autoQuarantineRuleRepository =
        mock(AutoQuarantineRuleRepository.class);
    when(autoQuarantineRuleRepository.findAll()).thenReturn(expectedAutoQuarantineRules);
    CachingDataStore underTest = new CachingDataStore(autoQuarantineRuleRepository);
    UUID testId = UUID.randomUUID();

    // When
    underTest.deleteQuarantineRule(testId.toString());

    // Then
    verify(autoQuarantineRuleRepository).deleteById(eq(testId));
  }
}
