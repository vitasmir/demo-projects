package cz.listek.backend.overdraft;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OverdraftInterestAccrualRepository extends JpaRepository<OverdraftInterestAccrual, UUID> {

    boolean existsByAccountIdAndInterestDate(UUID accountId, LocalDate interestDate);
}
