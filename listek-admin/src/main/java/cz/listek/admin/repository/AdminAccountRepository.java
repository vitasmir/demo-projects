package cz.listek.admin.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import cz.listek.admin.domain.AdminAccount;
import jakarta.persistence.LockModeType;

public interface AdminAccountRepository extends JpaRepository<AdminAccount, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<AdminAccount> findWithLockById(UUID id);
}
