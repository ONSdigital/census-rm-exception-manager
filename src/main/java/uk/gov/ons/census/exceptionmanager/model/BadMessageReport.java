package uk.gov.ons.census.exceptionmanager.model;

import lombok.Data;

@Data
public class BadMessageReport {
  private ExceptionReport exceptionReport;
  private ExceptionStats stats;
}
