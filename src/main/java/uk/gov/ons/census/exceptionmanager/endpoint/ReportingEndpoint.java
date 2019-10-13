package uk.gov.ons.census.exceptionmanager.endpoint;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.census.exceptionmanager.model.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.Peek;
import uk.gov.ons.census.exceptionmanager.model.Response;
import uk.gov.ons.census.exceptionmanager.model.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.persistence.InMemoryDatabase;

@RestController
public final class ReportingEndpoint {
  private final InMemoryDatabase inMemoryDatabase;

  public ReportingEndpoint(InMemoryDatabase inMemoryDatabase) {
    this.inMemoryDatabase = inMemoryDatabase;
  }

  @PostMapping(path = "/reportexception")
  public ResponseEntity<Response> reportError(@RequestBody ExceptionReport exceptionReport) {
    Response result = new Response();
    String messageHash = exceptionReport.getMessageHash();

    result.setSkipIt(inMemoryDatabase.shouldWeSkipThisMessage(messageHash));
    result.setPeek(inMemoryDatabase.shouldWePeekThisMessage(messageHash));
    result.setLogIt(inMemoryDatabase.shouldWeLogThisMessage(exceptionReport));

    inMemoryDatabase.updateStats(exceptionReport);

    return ResponseEntity.status(HttpStatus.OK).body(result);
  }

  @PostMapping(path = "/peekreply")
  public void reportError(@RequestBody Peek peekReply) {
    inMemoryDatabase.storePeekMessageReply(peekReply);
  }

  @PostMapping(path = "/storeskippedmessage")
  public void reportError(@RequestBody SkippedMessage skippedMessage) {
    inMemoryDatabase.storeSkippedMessage(skippedMessage);
  }
}
