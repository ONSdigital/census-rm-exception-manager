package uk.gov.ons.census.exceptionmanager.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Data;

@Data
public class ExceptionStats {
  Instant firstSeen = Instant.now();
  Instant lastSeen = Instant.now();
  AtomicInteger seenCount = new AtomicInteger(0);
}
