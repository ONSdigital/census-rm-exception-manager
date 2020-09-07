package uk.gov.ons.census.exceptionmanager.model.dto;

import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class AutoQuarantineRule {
  private String expression;
  private boolean doNotLog;
  private boolean quarantine;
  private boolean throwAway;
  private OffsetDateTime ruleExpiryDateTime;
}
