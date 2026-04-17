package uk.gov.ons.census.exceptionmanager.model.dto;

import lombok.Data;

@Data
public class Response {
  private boolean peek = false;
  private boolean logIt = false;
  private boolean skipIt = false;
  private boolean throwAway = false; // Don't log, don't quarantine
}
