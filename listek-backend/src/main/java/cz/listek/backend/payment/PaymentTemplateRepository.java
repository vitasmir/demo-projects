package cz.listek.backend.payment;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTemplateRepository extends JpaRepository<PaymentTemplate, UUID> {

    List<PaymentTemplate> findByAccount_IdOrderByCreatedAtDesc(UUID accountId);
}
