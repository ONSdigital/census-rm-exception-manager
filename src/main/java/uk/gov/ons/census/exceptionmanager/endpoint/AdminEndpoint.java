package uk.gov.ons.census.exceptionmanager.endpoint;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.census.exceptionmanager.model.BadMessageReport;
import uk.gov.ons.census.exceptionmanager.model.ExceptionStats;
import uk.gov.ons.census.exceptionmanager.model.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.persistence.InMemoryDatabase;

@RestController
public class AdminEndpoint {
  private final int PEEK_TIMEOUT = 10; // seconds

  private final InMemoryDatabase inMemoryDatabase;

  public AdminEndpoint(InMemoryDatabase inMemoryDatabase) {
    this.inMemoryDatabase = inMemoryDatabase;
  }

  @GetMapping(path = "/badmessages")
  public ResponseEntity<Set<String>> getBadMessages() {
    return ResponseEntity.status(HttpStatus.OK).body(inMemoryDatabase.getSeenHashes());
  }

  @GetMapping(path = "/badmessage/{messageHash}")
  public ResponseEntity<BadMessageReport> getBadMessageDetails(
      @PathVariable("messageHash") String messageHash) {
    ExceptionStats aggregateExceptionStats = new ExceptionStats();
    List<ExceptionStats> exceptionStatsList =
        inMemoryDatabase.getExceptionStats(messageHash, aggregateExceptionStats);

    BadMessageReport badMessageReport = new BadMessageReport();
    badMessageReport.setExceptionReports(inMemoryDatabase.getSeenExceptionReports(messageHash));
    badMessageReport.setExceptionStatsList(exceptionStatsList);
    badMessageReport.setExceptionStats(aggregateExceptionStats);

    return ResponseEntity.status(HttpStatus.OK).body(badMessageReport);
  }

  @GetMapping(path = "/skipmessage/{messageHash}")
  public void skipMessage(@PathVariable("messageHash") String messageHash) {
    inMemoryDatabase.skipMessage(messageHash);
  }

  @GetMapping(path = "/peekmessage/{messageHash}")
  public ResponseEntity<String> peekMessage(@PathVariable("messageHash") String messageHash) {
    inMemoryDatabase.peekMessage(messageHash);

    byte[] message;
    Instant timeOutTime = Instant.now().plus(Duration.ofSeconds(PEEK_TIMEOUT));
    while ((message = inMemoryDatabase.getPeekedMessage(messageHash)) == null) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        break;
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
  public ResponseEntity<Map<String, List<SkippedMessage>>> getSkippedMessages() {
    return ResponseEntity.status(HttpStatus.OK).body(inMemoryDatabase.getSkippedMessages());
  }

  @GetMapping(path = "/reset")
  public void reset() {
    inMemoryDatabase.reset();
  }
}
