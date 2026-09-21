package cz.listek.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import cz.listek.admin.domain.AdminUser;

public interface AdminUserRepository extends JpaRepository<AdminUser, String> {
}
