package uk.gov.ons.census.exceptionmanager.model;

import lombok.Data;

@Data
public class Peek {
  private String messageHash;
  private byte[] messagePayload;
}
