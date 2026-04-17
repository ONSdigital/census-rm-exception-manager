package uk.gov.ons.census.exceptionmanager.model.dto;

import lombok.Data;

@Data
public class SkipMessageRequest {
  private String messageHash;
  private String skippingUser;
}
