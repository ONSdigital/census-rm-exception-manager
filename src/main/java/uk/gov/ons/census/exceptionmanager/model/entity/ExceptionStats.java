package uk.gov.ons.census.exceptionmanager.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import javax.persistence.Column;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@RedisHash("ExceptionStats")
public class ExceptionStats implements Serializable {
  @Id private UUID id;
  @Column private Instant lastSeen;
  @Column private Instant firstSeen;
  @Column private AtomicInteger seenCount;
}
