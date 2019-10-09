package uk.gov.ons.census.exceptionmanager.model;

import java.util.Map;
import lombok.Data;

@Data
public class ExceptionReport {
  private String messageHash;
  private String service;
  private String queue;
  private String exceptionClass;
  private String exceptionMessage;
}
