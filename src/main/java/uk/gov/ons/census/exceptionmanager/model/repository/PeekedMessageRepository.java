package uk.gov.ons.census.exceptionmanager.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.exceptionmanager.model.entity.PeekedMessages;

import java.util.UUID;

public interface PeekedMessageRepository extends JpaRepository<PeekedMessages, UUID> {}
