package uk.gov.ons.census.exceptionmanager.endpoint;

import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.census.exceptionmanager.persistence.InMemoryDatabase;

@RestController
public class AdminEndpoint {
  private final int PEEK_TIMEOUT = 10000; // milliseconds

  private final InMemoryDatabase inMemoryDatabase;

  public AdminEndpoint(InMemoryDatabase inMemoryDatabase) {
    this.inMemoryDatabase = inMemoryDatabase;
  }

  @GetMapping(path = "/badmessages")
  public ResponseEntity<Set<String>> getBadMessages() {
    return ResponseEntity.status(HttpStatus.OK).body(inMemoryDatabase.getSeenHashes());
  }

  @GetMapping(path = "/skipmessage/{messageHash}")
  public void skipMessage(@PathVariable("messageHash") String messageHash) {
    inMemoryDatabase.skipMessage(messageHash);
  }

  @GetMapping(path = "/peekmessage/{messageHash}")
  public ResponseEntity<String> peekMessage(@PathVariable("messageHash") String messageHash) {
    inMemoryDatabase.peekMessage(messageHash);

    byte[] message;
    int maxWait = PEEK_TIMEOUT;
    while ((message = inMemoryDatabase.getPeekedMessage(messageHash)) == null) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        // Ignored
      }

      if (maxWait-- <= 0) {
        // Time out and return 404
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
      }
    }

    return ResponseEntity.status(HttpStatus.OK).body(new String(message));
  }

  @GetMapping(path = "/reset")
  public void reset() {
    inMemoryDatabase.reset();
  }
}
