package uk.gov.ons.census.exceptionmanager.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import javax.persistence.Column;
import java.io.Serializable;
import java.util.UUID;

@Data
@RedisHash("PeekedMessages")
public class PeekedMessages implements Serializable {
  @Id
  private UUID id;
  @Column
  private String messageHash;
  @Column
  private byte[] messagePayload;
}
