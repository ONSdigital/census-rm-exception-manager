package uk.gov.ons.census.exceptionmanager.model.entity;

import java.time.OffsetDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;

@Data
@Entity
public class AutoQuarantineRule {
  @Id private UUID id;

  @Column private String expression;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
  private boolean doNotLog;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
  private boolean quarantine;

  @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT false")
  private boolean throwAway;

  @Column(columnDefinition = "timestamp with time zone")
  private OffsetDateTime ruleExpiryDateTime;
}
