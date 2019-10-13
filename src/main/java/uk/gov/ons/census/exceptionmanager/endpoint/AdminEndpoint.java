package uk.gov.ons.census.exceptionmanager.endpoint;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.census.exceptionmanager.model.BadMessageReport;
import uk.gov.ons.census.exceptionmanager.model.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.persistence.InMemoryDatabase;

@RestController
public class AdminEndpoint {
  private final InMemoryDatabase inMemoryDatabase;

  @Value("${peek.timeout}")
  private int peekTimeout;

  public AdminEndpoint(InMemoryDatabase inMemoryDatabase) {
    this.inMemoryDatabase = inMemoryDatabase;
  }

  @GetMapping(path = "/badmessages")
  public ResponseEntity<Set<String>> getBadMessages() {
    return ResponseEntity.status(HttpStatus.OK).body(inMemoryDatabase.getSeenMessageHashes());
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
