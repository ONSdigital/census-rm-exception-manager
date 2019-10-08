package uk.gov.ons.census.exceptionmanager.model;

import lombok.Data;

@Data
public class SkippedMessage {
  private String messageHash;
  private byte[] messagePayload;
  private String service;
  private String queue;
}
