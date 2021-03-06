package uk.gov.ons.census.exceptionmanager.endpoint;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.ons.census.exceptionmanager.model.dto.AutoQuarantineRule;
import uk.gov.ons.census.exceptionmanager.model.dto.BadMessageReport;
import uk.gov.ons.census.exceptionmanager.model.dto.BadMessageSummary;
import uk.gov.ons.census.exceptionmanager.model.dto.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.model.entity.QuarantinedMessage;
import uk.gov.ons.census.exceptionmanager.model.repository.QuarantinedMessageRepository;
import uk.gov.ons.census.exceptionmanager.persistence.CachingDataStore;

@RestController
public class AdminEndpoint {
  private final CachingDataStore cachingDataStore;
  private final int peekTimeout;
  private final QuarantinedMessageRepository quarantinedMessageRepository;
  private final RabbitTemplate rabbitTemplate;

  public AdminEndpoint(
      CachingDataStore cachingDataStore,
      @Value("${peek.timeout}") int peekTimeout,
      QuarantinedMessageRepository quarantinedMessageRepository,
      RabbitTemplate rabbitTemplate) {
    this.cachingDataStore = cachingDataStore;
    this.peekTimeout = peekTimeout;
    this.quarantinedMessageRepository = quarantinedMessageRepository;
    this.rabbitTemplate = rabbitTemplate;
  }

  @GetMapping(path = "/badmessages")
  public ResponseEntity<Set<String>> getBadMessages(
      @RequestParam(value = "minimumSeenCount", required = false, defaultValue = "-1")
          int minimumSeenCount) {
    return ResponseEntity.status(HttpStatus.OK).body(getSeenMessageHashes(minimumSeenCount));
  }

  @GetMapping(path = "/badmessages/summary")
  public ResponseEntity<List<BadMessageSummary>> getBadMessagesSummary(
      @RequestParam(value = "minimumSeenCount", required = false, defaultValue = "-1")
          int minimumSeenCount) {
    List<BadMessageSummary> badMessageSummaryList = new LinkedList<>();
    Set<String> hashes = getSeenMessageHashes(minimumSeenCount);

    for (String messageHash : hashes) {
      BadMessageSummary badMessageSummary = new BadMessageSummary();
      badMessageSummary.setMessageHash(messageHash);
      badMessageSummaryList.add(badMessageSummary);

      Instant firstSeen = Instant.MAX;
      Instant lastSeen = Instant.MIN;
      int seenCount = 0;
      Set<String> affectedServices = new HashSet<>();
      Set<String> affectedQueues = new HashSet<>();

      for (BadMessageReport badMessageReport : cachingDataStore.getBadMessageReports(messageHash)) {
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
      badMessageSummary.setQuarantined(cachingDataStore.isQuarantined(messageHash));
    }

    return ResponseEntity.status(HttpStatus.OK).body(badMessageSummaryList);
  }

  @GetMapping(path = "/badmessage/{messageHash}")
  public ResponseEntity<List<BadMessageReport>> getBadMessageDetails(
      @PathVariable("messageHash") String messageHash) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(cachingDataStore.getBadMessageReports(messageHash));
  }

  @GetMapping(path = "/skipmessage/{messageHash}")
  public void skipMessage(@PathVariable("messageHash") String messageHash) {
    cachingDataStore.skipMessage(messageHash);
  }

  @GetMapping(path = "/peekmessage/{messageHash}")
  public ResponseEntity<String> peekMessage(@PathVariable("messageHash") String messageHash) {
    cachingDataStore.peekMessage(messageHash);

    byte[] message;
    Instant timeOutTime = Instant.now().plus(Duration.ofMillis(peekTimeout));
    while ((message = cachingDataStore.getPeekedMessage(messageHash)) == null) {
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
    return ResponseEntity.status(HttpStatus.OK).body(cachingDataStore.getAllSkippedMessages());
  }

  @GetMapping(path = "/skippedmessage/{messageHash}")
  public ResponseEntity<List<SkippedMessage>> getSkippedMessage(
      @PathVariable("messageHash") String messageHash) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(cachingDataStore.getSkippedMessages(messageHash));
  }

  @GetMapping(path = "/reset")
  public void reset(
      @RequestParam(value = "lastSeenCutoffSeconds", required = false)
          Optional<Integer> lastSeenCutoffSeconds) {
    cachingDataStore.reset(lastSeenCutoffSeconds);
  }

  @GetMapping(path = "/quarantinerule")
  public ResponseEntity<List<AutoQuarantineRule>> getQuarantineRules() {
    List<uk.gov.ons.census.exceptionmanager.model.entity.AutoQuarantineRule> quarantineRules =
        cachingDataStore.getQuarantineRules();
    List<AutoQuarantineRule> result =
        quarantineRules.stream()
            .map(
                rule -> {
                  AutoQuarantineRule mappedRule = new AutoQuarantineRule();
                  mappedRule.setExpression(rule.getExpression());
                  mappedRule.setQuarantine(rule.isQuarantine());
                  mappedRule.setSuppressLogging(rule.isSuppressLogging());
                  mappedRule.setThrowAway(rule.isThrowAway());
                  mappedRule.setRuleExpiryDateTime(rule.getRuleExpiryDateTime());
                  return mappedRule;
                })
            .collect(Collectors.toList());
    return ResponseEntity.status(HttpStatus.OK).body(result);
  }

  @Transactional
  @PostMapping(path = "/quarantinerule")
  public void addQuarantineRule(@RequestBody AutoQuarantineRule autoQuarantineRule) {
    cachingDataStore.addQuarantineRuleExpression(
        autoQuarantineRule.getExpression(),
        autoQuarantineRule.isSuppressLogging(),
        autoQuarantineRule.isQuarantine(),
        autoQuarantineRule.isThrowAway(),
        autoQuarantineRule.getRuleExpiryDateTime());
  }

  @Transactional
  @DeleteMapping(path = "/quarantinerule/{id}")
  public void deleteQuarantineRules(@PathVariable("id") String id) {
    cachingDataStore.deleteQuarantineRule(id);
  }

  @Transactional
  @GetMapping(path = "/replayquarantinedmessage/{id}")
  public void replaySkippedMessage(@PathVariable("id") String id) {
    Optional<QuarantinedMessage> quarantinedMessageOpt =
        quarantinedMessageRepository.findById(UUID.fromString(id));
    if (!quarantinedMessageOpt.isPresent()) {
      throw new RuntimeException("Cannot find quarantined message with ID: " + id);
    }

    QuarantinedMessage quarantinedMessage = quarantinedMessageOpt.get();

    if (quarantinedMessage.getRoutingKey().equals("none")) {
      throw new RuntimeException("Cannot replay PubSub messages... yet"); // TODO
    }

    MessagePostProcessor mpp =
        message -> {
          message.getMessageProperties().setDeliveryMode(MessageDeliveryMode.PERSISTENT);
          message.getMessageProperties().setContentType(quarantinedMessage.getContentType());
          for (Entry<String, JsonNode> headerEntry : quarantinedMessage.getHeaders().entrySet()) {
            message.getMessageProperties().setHeader(headerEntry.getKey(), headerEntry.getValue());
          }
          return message;
        };

    rabbitTemplate.convertAndSend(
        quarantinedMessage.getQueue(), quarantinedMessage.getMessagePayload(), mpp);

    quarantinedMessageRepository.delete(quarantinedMessage);
  }

  private Set<String> getSeenMessageHashes(int minimumSeenCount) {
    Set<String> hashes;

    // -1 means "no minimum"
    if (minimumSeenCount == -1) {
      hashes = cachingDataStore.getSeenMessageHashes();
    } else {
      hashes = cachingDataStore.getSeenMessageHashes(minimumSeenCount);
    }

    return hashes;
  }
}
