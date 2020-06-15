package uk.gov.ons.census.exceptionmanager.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.exceptionmanager.model.entity.ExceptionStats;

import java.util.UUID;

public interface ExceptionStatsRepository extends JpaRepository<ExceptionStats, UUID> {}
