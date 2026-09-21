package cz.listek.backend.account;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record AccountResponse(UUID id, String ownerName, String username, String firstName, String lastName,
            String birthNumber, String email, String address, String accountNumber, BigDecimal balance,
            CurrencyCode currency, AccountType type) {

    }

    public record CreateAccountRequest(
            @NotBlank String ownerName,
            @NotBlank
            @jakarta.validation.constraints.Email String email,
            @NotBlank
            @Size(max = 240) String address,
            @NotBlank
            @Size(min = 8, max = 120) String password,
            @NotBlank
            @Pattern(regexp = "[0-9]{9,34}") String accountNumber,
            BigDecimal initialBalance,
            CurrencyCode currency) {

    }

    public record UpdateAccountRequest(@NotBlank
            @Size(max = 120) String ownerName,
            @NotBlank
            @jakarta.validation.constraints.Email String email,
            @NotBlank
            @Size(max = 240) String address,
            @Size(min = 8, max = 120) String password) {

    }

    public record RegisterAccountRequest(
            @NotBlank
            @Size(max = 80) String username,
            @NotBlank
            @Size(max = 80) String firstName,
            @NotBlank
            @Size(max = 80) String lastName,
            @NotBlank
            @Pattern(regexp = "[0-9]{6}/?[0-9]{3,4}") String birthNumber,
            @NotBlank
            @jakarta.validation.constraints.Email String email,
            @NotBlank
            @Size(max = 120) String street,
            @NotBlank
            @Size(max = 100) String city,
            @NotBlank
            @Pattern(regexp = "[0-9]{3} ?[0-9]{2}") String postalCode,
            @NotBlank
            @Size(min = 12, max = 200) String password) {

    }
}
