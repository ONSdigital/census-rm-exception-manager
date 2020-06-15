package uk.gov.ons.census.exceptionmanager.model.entity;

import lombok.Data;
import org.springframework.data.redis.core.RedisHash;

import javax.persistence.Column;
import javax.persistence.Id;
import java.io.Serializable;
import java.util.UUID;

@Data
@RedisHash("ExceptionReports")
public class ExceptionReport implements Serializable {
  @Id
  private UUID id;
  @Column
  private String messageHash;
  @Column private String service;
  @Column private String queue;
  @Column private String exceptionClass;
  @Column private String exceptionMessage;
}

