package cz.listek.admin.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.listek.admin.domain.ApplicationStatus;
import cz.listek.admin.domain.OverdraftApplication;

public interface OverdraftApplicationRepository extends JpaRepository<OverdraftApplication, UUID> {

    List<OverdraftApplication> findAllByOrderByCreatedAtDesc();

    long countByStatus(ApplicationStatus status);

    boolean existsByAccount_Id(UUID accountId);
}
