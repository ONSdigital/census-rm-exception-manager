package uk.gov.ons.census.exceptionmanager.model.entity;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Data
@Entity
public class Redis {
    @Id
    private UUID id;

    @Column private String messageHash;

    @Column private String service;

    @Column private String queue;

    @Column private String exceptionClass;

    @Column private String exceptionMessage;

    @Column private Instant firstSeen = Instant.now();

    @Column private Instant lastSeen = Instant.now();

    @Column private AtomicInteger seenCount = new AtomicInteger(1);

    @Column(columnDefinition = "timestamp with time zone")
    @CreationTimestamp
    private OffsetDateTime skippedTimestamp;

    @Column private byte[] messagePayload;

    @Column private String routingKey;

    @Column private String contentType;

    @Type(type = "jsonb")
    @Column(columnDefinition = "jsonb")
    private Map<String, JsonNode> headers;



}
