package cz.listek.backend.account;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByEmailIgnoreCase(String email);

    Optional<Account> findByEmailIgnoreCaseAndType(String email, AccountType type);

    Optional<Account> findByUsernameIgnoreCaseAndType(String username, AccountType type);

    boolean existsByEmailIgnoreCase(String email);

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    boolean existsByEmailIgnoreCaseAndType(String email, AccountType type);

    boolean existsByUsernameIgnoreCaseAndType(String username, AccountType type);
}
