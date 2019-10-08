package uk.gov.ons.census.exceptionmanager.model;

import lombok.Data;

@Data
public class Response {
  private boolean peek = false;
  private boolean logIt = false;
  private boolean skipIt = false;
}
