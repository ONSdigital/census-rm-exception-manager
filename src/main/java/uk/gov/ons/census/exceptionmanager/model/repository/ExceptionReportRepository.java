package uk.gov.ons.census.exceptionmanager.model.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.ons.census.exceptionmanager.model.entity.ExceptionReport;

import java.util.UUID;

public interface ExceptionReportRepository extends JpaRepository<ExceptionReport, UUID> {}
