package cz.listek.backend.card;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BankCardRepository extends JpaRepository<BankCard, UUID> {

    List<BankCard> findByAccount_IdOrderByIdDesc(UUID accountId);
}
