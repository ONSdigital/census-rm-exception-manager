package uk.gov.ons.census.exceptionmanager.model.entity;

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
}
