package uk.gov.ons.census.exceptionmanager.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;

@Data
public class ExceptionStats {
  private Instant firstSeen = Instant.now();
  private Instant lastSeen = Instant.now();
  private AtomicInteger seenCount = new AtomicInteger(1);
}
