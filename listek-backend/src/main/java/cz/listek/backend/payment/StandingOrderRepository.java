package cz.listek.backend.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

public interface StandingOrderRepository extends JpaRepository<StandingOrder, UUID> {

    List<StandingOrder> findByAccount_IdOrderByCreatedAtDesc(UUID accountId);

    List<StandingOrder> findByActiveTrue();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<StandingOrder> findWithLockById(UUID id);
}
