package uk.gov.ons.census.exceptionmanager.model.entity;

import lombok.Data;
import org.springframework.data.redis.core.RedisHash;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@RedisHash("BadMessageSummaries")
public class BadMessageSummary implements Serializable {
  @Id private UUID id;

  @Column private String messageHash;
  @Column private Instant firstSeen;
  @Column private Instant lastSeen;
  @Column private AtomicInteger seenCount;
  @Column private Set<String> affectedServices;
  @Column private Set<String> affectedQueues;
  @Column private boolean quarantined;

  //    @Id
  //    private UUID id;
  //
  //    @Column private String messageHash;
  //
  //    @Column private String service;
  //
  //    @Column private String queue;
  //
  //    @Column private String exceptionClass;
  //
  //    @Column private String exceptionMessage;
  //
  //
  //    @Column private AtomicInteger seenCount = new AtomicInteger(1);
  //
  //    @Column(columnDefinition = "timestamp with time zone")
  //    @CreationTimestamp
  //    private OffsetDateTime skippedTimestamp;
  //
  //    @Column private byte[] messagePayload;
  //
  //    @Column private String routingKey;
  //
  //    @Column private String contentType;
  //
  //    @Type(type = "jsonb")
  //    @Column(columnDefinition = "jsonb")
  //    private Map<String, JsonNode> headers;

}
