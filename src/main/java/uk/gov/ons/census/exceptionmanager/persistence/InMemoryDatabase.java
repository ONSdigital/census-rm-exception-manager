package uk.gov.ons.census.exceptionmanager.persistence;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.exceptionmanager.model.BadMessageReport;
import uk.gov.ons.census.exceptionmanager.model.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.ExceptionStats;
import uk.gov.ons.census.exceptionmanager.model.Peek;
import uk.gov.ons.census.exceptionmanager.model.SkippedMessage;

@Component
public class InMemoryDatabase {
  private Map<ExceptionReport, ExceptionStats> seenExceptions = new HashMap<>();
  private Map<String, List<ExceptionReport>> messageExceptionReports = new HashMap<>();
  private Set<String> messagesToSkip = new HashSet<>();
  private Set<String> messagesToPeek = new HashSet<>();
  private Map<String, byte[]> peekedMessages = new HashMap<>();
  private Map<String, List<SkippedMessage>> skippedMessages = new HashMap<>();

  public synchronized void updateStats(ExceptionReport exceptionReport) {
    String messageHash = exceptionReport.getMessageHash();

    ExceptionStats exceptionStats = seenExceptions.get(exceptionReport);

    if (exceptionStats != null) {
      exceptionStats.getSeenCount().incrementAndGet();
      exceptionStats.setLastSeen(Instant.now());
      return;
    }

    seenExceptions.put(exceptionReport, new ExceptionStats());

    List<ExceptionReport> exceptionReportList =
        messageExceptionReports.computeIfAbsent(messageHash, key -> new LinkedList<>());
    exceptionReportList.add(exceptionReport);
  }

  public boolean shouldWeLogThisMessage(ExceptionReport exceptionReport) {
    return seenExceptions.get(exceptionReport) == null;
  }

  public boolean shouldWeSkipThisMessage(String messageHash) {
    return messagesToSkip.contains(messageHash);
  }

  public boolean shouldWePeekThisMessage(String messageHash) {
    return messagesToPeek.contains(messageHash);
  }

  public Set<String> getSeenMessageHashes() {
    return messageExceptionReports.keySet();
  }

  public void skipMessage(String messageHash) {
    messagesToSkip.add(messageHash);
  }

  public void peekMessage(String messageHash) {
    messagesToPeek.add(messageHash);
  }

  public synchronized void storePeekMessageReply(Peek peekReply) {
    peekedMessages.put(peekReply.getMessageHash(), peekReply.getMessagePayload());

    // We don't want services to keep sending us the 'peek'ed message now we've got it
    messagesToPeek.remove(peekReply.getMessageHash());
  }

  public synchronized void storeSkippedMessage(SkippedMessage skippedMessage) {
    // TODO: Persist this to a Rabbit queue or a database so it can be replayed if necessary
    // Make damn certain this is thread safe so we don't lose anything
    List<SkippedMessage> skippedMessageList =
        skippedMessages.computeIfAbsent(skippedMessage.getMessageHash(), key -> new LinkedList<>());
    skippedMessageList.add(skippedMessage);
  }

  public byte[] getPeekedMessage(String messageHash) {
    return peekedMessages.get(messageHash);
  }

  public List<BadMessageReport> getBadMessageReports(String messageHash) {
    List<BadMessageReport> results = new LinkedList<>();
    List<ExceptionReport> exceptionReportList = messageExceptionReports.get(messageHash);

    if (exceptionReportList == null) {
      return Collections.emptyList();
    }

    for (ExceptionReport exceptionReport : exceptionReportList) {
      BadMessageReport badMessageReport = new BadMessageReport();
      badMessageReport.setExceptionReport(exceptionReport);
      badMessageReport.setStats(seenExceptions.get(exceptionReport));
      results.add(badMessageReport);
    }

    return results;
  }

  public Map<String, List<SkippedMessage>> getAllSkippedMessages() {
    return skippedMessages;
  }

  public List<SkippedMessage> getSkippedMessages(String messageHash) {
    return skippedMessages.get(messageHash);
  }

  public void reset() {
    seenExceptions.clear();
    messageExceptionReports.clear();
    messagesToSkip.clear();
    messagesToPeek.clear();
    peekedMessages.clear();
  }
}
