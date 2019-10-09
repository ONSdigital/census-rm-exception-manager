package uk.gov.ons.census.exceptionmanager.model;

import java.util.Set;
import lombok.Data;

@Data
public class BadMessageReport {
  ExceptionStats exceptionStats;
  Set<ExceptionReport> exceptionReports;
}
