package uk.gov.ons.census.exceptionmanager.endpoint;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.census.exceptionmanager.model.dto.BadMessageReport;
import uk.gov.ons.census.exceptionmanager.model.dto.BadMessageSummary;
import uk.gov.ons.census.exceptionmanager.model.dto.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.persistence.InMemoryDatabase;

@RestController
public class AdminEndpoint {
  private final InMemoryDatabase inMemoryDatabase;
  private final int peekTimeout;

  public AdminEndpoint(
      InMemoryDatabase inMemoryDatabase, @Value("${peek.timeout}") int peekTimeout) {
    this.inMemoryDatabase = inMemoryDatabase;
    this.peekTimeout = peekTimeout;
  }

  @GetMapping(path = "/badmessages")
  public ResponseEntity<Set<String>> getBadMessages() {
    return ResponseEntity.status(HttpStatus.OK).body(inMemoryDatabase.getSeenMessageHashes());
  }

  @GetMapping(path = "/badmessages/summary")
  public ResponseEntity<List<BadMessageSummary>> getBadMessagesSummary() {
    List<BadMessageSummary> badMessageSummaryList = new LinkedList<>();
    for (String messageHash : inMemoryDatabase.getSeenMessageHashes()) {
      BadMessageSummary badMessageSummary = new BadMessageSummary();
      badMessageSummary.setMessageHash(messageHash);
      badMessageSummaryList.add(badMessageSummary);

      Instant firstSeen = Instant.MAX;
      Instant lastSeen = Instant.MIN;
      int seenCount = 0;
      Set<String> affectedServices = new HashSet<>();
      Set<String> affectedQueues = new HashSet<>();

      for (BadMessageReport badMessageReport : inMemoryDatabase.getBadMessageReports(messageHash)) {
        if (badMessageReport.getStats().getFirstSeen().isBefore(firstSeen)) {
          firstSeen = badMessageReport.getStats().getFirstSeen();
        }

        if (badMessageReport.getStats().getLastSeen().isAfter(lastSeen)) {
          lastSeen = badMessageReport.getStats().getLastSeen();
        }

        seenCount += badMessageReport.getStats().getSeenCount().get();

        affectedServices.add(badMessageReport.getExceptionReport().getService());
        affectedQueues.add(badMessageReport.getExceptionReport().getQueue());
      }

      badMessageSummary.setFirstSeen(firstSeen);
      badMessageSummary.setLastSeen(lastSeen);
      badMessageSummary.setSeenCount(seenCount);
      badMessageSummary.setAffectedServices(affectedServices);
      badMessageSummary.setAffectedQueues(affectedQueues);
      badMessageSummary.setQuarantined(inMemoryDatabase.shouldWeSkipThisMessage(messageHash));
    }

    return ResponseEntity.status(HttpStatus.OK).body(badMessageSummaryList);
  }

  @GetMapping(path = "/badmessage/{messageHash}")
  public ResponseEntity<List<BadMessageReport>> getBadMessageDetails(
      @PathVariable("messageHash") String messageHash) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(inMemoryDatabase.getBadMessageReports(messageHash));
  }

  @GetMapping(path = "/skipmessage/{messageHash}")
  public void skipMessage(@PathVariable("messageHash") String messageHash) {
    inMemoryDatabase.skipMessage(messageHash);
  }

  @GetMapping(path = "/peekmessage/{messageHash}")
  public ResponseEntity<String> peekMessage(@PathVariable("messageHash") String messageHash) {
    inMemoryDatabase.peekMessage(messageHash);

    byte[] message;
    Instant timeOutTime = Instant.now().plus(Duration.ofMillis(peekTimeout));
    while ((message = inMemoryDatabase.getPeekedMessage(messageHash)) == null) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        break; // Service must be shutting down, probably
      }

      if (Instant.now().isAfter(timeOutTime)) {
        break;
      }
    }

    if (message == null) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
    } else {
      return ResponseEntity.status(HttpStatus.OK).body(new String(message));
    }
  }

  @GetMapping(path = "/skippedmessages")
  public ResponseEntity<Map<String, List<SkippedMessage>>> getAllSkippedMessages() {
    return ResponseEntity.status(HttpStatus.OK).body(inMemoryDatabase.getAllSkippedMessages());
  }

  @GetMapping(path = "/skippedmessage/{messageHash}")
  public ResponseEntity<List<SkippedMessage>> getSkippedMessage(
      @PathVariable("messageHash") String messageHash) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(inMemoryDatabase.getSkippedMessages(messageHash));
  }

  @GetMapping(path = "/reset")
  public void reset() {
    inMemoryDatabase.reset();
  }
}
