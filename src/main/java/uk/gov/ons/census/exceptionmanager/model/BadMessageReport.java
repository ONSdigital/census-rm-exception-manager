package uk.gov.ons.census.exceptionmanager.model;

import java.util.List;
import lombok.Data;

@Data
public class BadMessageReport {
  ExceptionStats exceptionStats;
  List<ExceptionReport> exceptionReports;
}
