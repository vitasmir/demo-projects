package cz.listek.admin.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.listek.admin.domain.AdminTransaction;

public interface AdminTransactionRepository extends JpaRepository<AdminTransaction, UUID> {
}
