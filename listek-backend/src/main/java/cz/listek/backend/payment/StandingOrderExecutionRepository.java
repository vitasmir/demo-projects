package cz.listek.backend.payment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StandingOrderExecutionRepository extends JpaRepository<StandingOrderExecution, UUID> {
}
