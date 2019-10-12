package uk.gov.ons.census.exceptionmanager.persistence;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.exceptionmanager.model.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.ExceptionStats;
import uk.gov.ons.census.exceptionmanager.model.Peek;
import uk.gov.ons.census.exceptionmanager.model.SkippedMessage;

@Component
public class InMemoryDatabase {
  private Map<String, ExceptionStats> seenExceptions = new HashMap<>();
  private Set<String> seenHashes = new HashSet<>();
  private Map<String, List<ExceptionReport>> seenExceptionReports = new HashMap<>();
  private Set<String> messagesToSkip = new HashSet<>();
  private Set<String> messagesToPeek = new HashSet<>();
  private Map<String, byte[]> peekedMessages = new HashMap<>();
  private Map<String, List<SkippedMessage>> skippedMessages = new HashMap<>();

  public synchronized boolean haveWeSeenThisExceptionBefore(ExceptionReport exceptionReport) {
    String messageHash = exceptionReport.getMessageHash();
    String uniqueIdentifier =
        String.format(
            "%s_%s_%s_%s_%s",
            messageHash,
            exceptionReport.getService(),
            exceptionReport.getQueue(),
            exceptionReport.getExceptionClass(),
            exceptionReport.getExceptionMessage());

    if (seenExceptions.containsKey(uniqueIdentifier)) {
      seenExceptions.get(uniqueIdentifier).getSeenCount().incrementAndGet();
      seenExceptions.get(uniqueIdentifier).setLastSeen(Instant.now());
      return true;
    }

    if (seenExceptionReports.containsKey(messageHash)) {
      seenExceptionReports.get(messageHash).add(exceptionReport);
    } else {
      List<ExceptionReport> list = new LinkedList<>();
      list.add(exceptionReport);
      seenExceptionReports.put(messageHash, list);
    }

    seenExceptions.put(uniqueIdentifier, new ExceptionStats());
    seenHashes.add(messageHash);
    return false;
  }

  public boolean shouldWeSkipThisMessage(String messageHash) {
    return messagesToSkip.contains(messageHash);
  }

  public boolean shouldWePeekThisMessage(String messageHash) {
    return messagesToPeek.contains(messageHash);
  }

  public Set<String> getSeenHashes() {
    return seenHashes;
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

  public void storeSkippedMessage(SkippedMessage skippedMessage) {
    // TODO: Persist this to a Rabbit queue or a database so it can be replayed if necessary
    // Make damn certain this is thread safe so we don't lose anything
    synchronized (skippedMessages) {
      List<SkippedMessage> skippedMessageList =
          skippedMessages.computeIfAbsent(
              skippedMessage.getMessageHash(), key -> new LinkedList<>());
      skippedMessageList.add(skippedMessage);
    }
  }

  public byte[] getPeekedMessage(String messageHash) {
    return peekedMessages.get(messageHash);
  }

  public List<ExceptionReport> getSeenExceptionReports(String messageHash) {
    return seenExceptionReports.get(messageHash);
  }

  public ExceptionStats getExceptionStats(String messageHash) {
    for (String uniqueIdentifier : seenExceptions.keySet()) {
      if (uniqueIdentifier.startsWith(messageHash)) {
        return seenExceptions.get(uniqueIdentifier);
      }
    }

    return null;
  }

  public Map<String, List<SkippedMessage>> getSkippedMessages() {
    return skippedMessages;
  }

  public void reset() {
    seenExceptions.clear();
    seenHashes.clear();
    seenExceptionReports.clear();
    messagesToSkip.clear();
    messagesToPeek.clear();
    peekedMessages.clear();
  }
}
