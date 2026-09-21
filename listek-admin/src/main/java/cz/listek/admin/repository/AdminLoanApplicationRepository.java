package cz.listek.admin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.listek.admin.domain.AdminLoanApplication;
import cz.listek.admin.domain.ApplicationStatus;

public interface AdminLoanApplicationRepository extends JpaRepository<AdminLoanApplication, UUID> {
    List<AdminLoanApplication> findAllByOrderByCreatedAtDesc();
    long countByStatus(ApplicationStatus status);
}