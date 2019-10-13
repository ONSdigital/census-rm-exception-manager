package uk.gov.ons.census.exceptionmanager.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode
public class ExceptionReport {
  private String messageHash;
  private String service;
  private String queue;
  private String exceptionClass;
  private String exceptionMessage;
}
