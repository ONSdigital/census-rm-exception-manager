package uk.gov.ons.census.exceptionmanager.model.repository;


import org.springframework.data.repository.CrudRepository;
import uk.gov.ons.census.exceptionmanager.model.entity.Redis;

import java.util.UUID;

public interface RedisRepository extends CrudRepository<Redis, UUID> {}
