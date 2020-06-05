package uk.gov.ons.census.exceptionmanager.model.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.exceptionmanager.model.entity.AutoQuarantineRule;

public interface AutoQuarantineRuleRepository extends JpaRepository<AutoQuarantineRule, UUID> {}
