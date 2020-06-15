package uk.gov.ons.census.exceptionmanager.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.exceptionmanager.model.entity.BadMessageSummary;

import java.util.UUID;

public interface BadMessageSummaryRepository extends JpaRepository<BadMessageSummary, UUID> {}
