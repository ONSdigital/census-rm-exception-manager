package uk.gov.ons.census.exceptionmanager.endpoint;

import java.util.UUID;
import javax.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.census.exceptionmanager.helper.JsonHelper;
import uk.gov.ons.census.exceptionmanager.model.dto.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.dto.Peek;
import uk.gov.ons.census.exceptionmanager.model.dto.Response;
import uk.gov.ons.census.exceptionmanager.model.dto.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.model.entity.QuarantinedMessage;
import uk.gov.ons.census.exceptionmanager.model.repository.QuarantinedMessageRepository;
import uk.gov.ons.census.exceptionmanager.persistence.InMemoryDatabase;

@RestController
public class ReportingEndpoint {
  private final InMemoryDatabase inMemoryDatabase;
  private final QuarantinedMessageRepository quarantinedMessageRepository;

  public ReportingEndpoint(
      InMemoryDatabase inMemoryDatabase,
      QuarantinedMessageRepository quarantinedMessageRepository) {
    this.inMemoryDatabase = inMemoryDatabase;
    this.quarantinedMessageRepository = quarantinedMessageRepository;
  }

  @PostMapping(path = "/reportexception")
  public ResponseEntity<Response> reportError(@RequestBody ExceptionReport exceptionReport) {
    Response result = new Response();
    String messageHash = exceptionReport.getMessageHash();

    result.setSkipIt(inMemoryDatabase.shouldWeSkipThisMessage(exceptionReport));
    result.setPeek(inMemoryDatabase.shouldWePeekThisMessage(messageHash));
    result.setLogIt(inMemoryDatabase.shouldWeLogThisMessage(exceptionReport));

    inMemoryDatabase.updateStats(exceptionReport);

    return ResponseEntity.status(HttpStatus.OK).body(result);
  }

  @PostMapping(path = "/peekreply")
  public void peekReply(@RequestBody Peek peekReply) {
    inMemoryDatabase.storePeekMessageReply(peekReply);
  }

  @Transactional
  @PostMapping(path = "/storeskippedmessage")
  public void storeSkippedMessage(@RequestBody SkippedMessage skippedMessage) {
    inMemoryDatabase.storeSkippedMessage(skippedMessage);
    String errorReports =
        JsonHelper.convertObjectToJson(
            inMemoryDatabase.getBadMessageReports(skippedMessage.getMessageHash()));

    QuarantinedMessage quarantinedMessage = new QuarantinedMessage();
    quarantinedMessage.setId(UUID.randomUUID());
    quarantinedMessage.setContentType(skippedMessage.getContentType());
    quarantinedMessage.setHeaders(skippedMessage.getHeaders());
    quarantinedMessage.setMessageHash(skippedMessage.getMessageHash());
    quarantinedMessage.setMessagePayload(skippedMessage.getMessagePayload());
    quarantinedMessage.setQueue(skippedMessage.getQueue());
    quarantinedMessage.setRoutingKey(skippedMessage.getRoutingKey());
    quarantinedMessage.setService(skippedMessage.getService());
    quarantinedMessage.setErrorReports(errorReports);

    quarantinedMessageRepository.save(quarantinedMessage);
  }
}
