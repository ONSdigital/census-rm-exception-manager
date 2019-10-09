package uk.gov.ons.census.exceptionmanager.persistence;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.exceptionmanager.model.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.Peek;
import uk.gov.ons.census.exceptionmanager.model.SkippedMessage;

@Component
public class InMemoryDatabase {
  private Set<String> seenExceptions = new HashSet<>();
  private Set<String> seenHashes = new HashSet<>();
  private Map<String, Set<ExceptionReport>> seenExceptionReports = new HashMap<>();
  private Set<String> messagesToSkip = new HashSet<>();
  private Set<String> messagesToPeek = new HashSet<>();
  private Map<String, byte[]> peekedMessages = new HashMap<>();
  private Map<String, SkippedMessage> skippedMessages = new HashMap<>();

  public boolean haveWeSeenThisExceptionBefore(ExceptionReport exceptionReport) {
    String messageHash = exceptionReport.getMessageHash();
    String uniqueIdentifier =
        String.format(
            "%s_%s_%s_%s_%s",
            messageHash,
            exceptionReport.getService(),
            exceptionReport.getQueue(),
            exceptionReport.getExceptionClass(),
            exceptionReport.getExceptionMessage());

    if (seenExceptions.contains(uniqueIdentifier)) {
      return true;
    }

    if (seenExceptionReports.containsKey(messageHash)) {
      seenExceptionReports.get(messageHash).add(exceptionReport);
    } else {
      seenExceptionReports.put(messageHash, Set.of(exceptionReport));
    }

    seenExceptions.add(uniqueIdentifier);
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

  public void storePeekMessageReply(Peek peekReply) {
    peekedMessages.put(peekReply.getMessageHash(), peekReply.getMessagePayload());

    // We don't want services to keep sending us the 'peek'ed message now we've got it
    messagesToPeek.remove(peekReply.getMessageHash());
  }

  public void storeSkippedMessage(SkippedMessage skippedMessage) {
    // TODO: Persist this to a Rabbit queue or a database so it can be replayed if necessary
    skippedMessages.put(skippedMessage.getMessageHash(), skippedMessage);
  }

  public byte[] getPeekedMessage(String messageHash) {
    return peekedMessages.get(messageHash);
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
