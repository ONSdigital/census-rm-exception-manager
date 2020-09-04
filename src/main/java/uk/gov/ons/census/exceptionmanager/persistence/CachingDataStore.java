package uk.gov.ons.census.exceptionmanager.persistence;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import uk.gov.ons.census.exceptionmanager.model.dto.BadMessageReport;
import uk.gov.ons.census.exceptionmanager.model.dto.ExceptionReport;
import uk.gov.ons.census.exceptionmanager.model.dto.ExceptionStats;
import uk.gov.ons.census.exceptionmanager.model.dto.Peek;
import uk.gov.ons.census.exceptionmanager.model.dto.SkippedMessage;
import uk.gov.ons.census.exceptionmanager.model.entity.AutoQuarantineRule;
import uk.gov.ons.census.exceptionmanager.model.repository.AutoQuarantineRuleRepository;

@Component
public class CachingDataStore {
  private static final Logger log = LoggerFactory.getLogger(CachingDataStore.class);
  private Map<ExceptionReport, ExceptionStats> seenExceptions = new HashMap<>();
  private Map<String, List<ExceptionReport>> messageExceptionReports = new HashMap<>();
  private Set<String> messagesToSkip = new HashSet<>();
  private Set<String> messagesToPeek = new HashSet<>();
  private Map<String, byte[]> peekedMessages = new HashMap<>();
  private Map<String, List<SkippedMessage>> skippedMessages = new HashMap<>();
  private List<Expression> autoQuarantineExpressions = new LinkedList<>();
  private final AutoQuarantineRuleRepository quarantineRuleRepository;
  private final int numberOfRetriesBeforeLogging;

  public CachingDataStore(
      AutoQuarantineRuleRepository quarantineRuleRepository,
      @Value("${general-config.number-of-retries-before-logging}")
          int numberOfRetriesBeforeLogging) {
    this.quarantineRuleRepository = quarantineRuleRepository;
    this.numberOfRetriesBeforeLogging = numberOfRetriesBeforeLogging;

    List<AutoQuarantineRule> autoQuarantineRules = quarantineRuleRepository.findAll();

    for (AutoQuarantineRule rule : autoQuarantineRules) {
      ExpressionParser expressionParser = new SpelExpressionParser();
      Expression spelExpression = expressionParser.parseExpression(rule.getExpression());
      autoQuarantineExpressions.add(spelExpression);
    }
  }

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
    ExceptionStats exceptionStats = seenExceptions.get(exceptionReport);

    if (numberOfRetriesBeforeLogging > 0) {
      // Don't log until we've seen the exception a [configurable] number of times
      if (exceptionStats != null
          && !exceptionStats.isLoggedAtLeastOnce()
          && exceptionStats.getSeenCount().get() > numberOfRetriesBeforeLogging - 1) {
        exceptionStats.setLoggedAtLeastOnce(true);
        return true;
      }

      return false;
    } else {
      return exceptionStats == null;
    }
  }

  public boolean shouldWeSkipThisMessage(ExceptionReport exceptionReport) {
    EvaluationContext context = new StandardEvaluationContext(exceptionReport);
    for (Expression expression : autoQuarantineExpressions) {
      Boolean expressionResult = expression.getValue(context, Boolean.class);
      if (expressionResult) {
        log.with("exception_report", exceptionReport)
            .with("expression", expression.getExpressionString())
            .warn("Auto-quarantine message rule matched");
        return true;
      }
    }

    return messagesToSkip.contains(exceptionReport.getMessageHash());
  }

  public boolean isQuarantined(String messageHash) {
    return messagesToSkip.contains(messageHash);
  }

  public boolean shouldWePeekThisMessage(String messageHash) {
    return messagesToPeek.contains(messageHash);
  }

  public Set<String> getSeenMessageHashes() {
    return messageExceptionReports.keySet();
  }

  public Set<String> getSeenMessageHashes(int minimumSeenCount) {
    Set<String> result = new HashSet<>();

    for (Entry<ExceptionReport, ExceptionStats> item : seenExceptions.entrySet()) {
      if (item.getValue().getSeenCount().get() >= minimumSeenCount) {
        result.add(item.getKey().getMessageHash());
      }
    }

    return result;
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

  public void addQuarantineRuleExpression(String expression) {
    ExpressionParser expressionParser = new SpelExpressionParser();
    Expression spelExpression = expressionParser.parseExpression(expression);

    AutoQuarantineRule autoQuarantineRule = new AutoQuarantineRule();
    autoQuarantineRule.setId(UUID.randomUUID());
    autoQuarantineRule.setExpression(expression);
    quarantineRuleRepository.saveAndFlush(autoQuarantineRule);

    autoQuarantineExpressions.add(spelExpression);
  }

  public List<AutoQuarantineRule> getQuarantineRules() {
    return quarantineRuleRepository.findAll();
  }

  public void deleteQuarantineRule(String id) {
    quarantineRuleRepository.deleteById(UUID.fromString(id));
    List<AutoQuarantineRule> rules = quarantineRuleRepository.findAll();
    List<Expression> newRules = new LinkedList<>();
    for (AutoQuarantineRule rule : rules) {
      ExpressionParser expressionParser = new SpelExpressionParser();
      Expression spelExpression = expressionParser.parseExpression(rule.getExpression());
      newRules.add(spelExpression);
    }

    autoQuarantineExpressions = newRules;
  }
}
