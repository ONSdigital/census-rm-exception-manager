package uk.gov.ons.census.exceptionmanager.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.exceptionmanager.model.entity.QuarantinedMessage;

public interface QuarantinedMessageRepository extends JpaRepository<QuarantinedMessage, UUID> {}
