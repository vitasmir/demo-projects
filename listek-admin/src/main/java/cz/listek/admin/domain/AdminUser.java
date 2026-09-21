package cz.listek.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_user")
public class AdminUser {

    @Id
    @Column(length = 80)
    private String username;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "birth_number", length = 11)
    private String birthNumber;

    @Column(length = 160)
    private String email;

    @Column(length = 160)
    private String street;

    @Column(length = 100)
    private String city;

    @Column(name = "postal_code", length = 6)
    private String postalCode;

    @Column(name = "password_hash", nullable = false, length = 64)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    protected AdminUser() {
    }

    public AdminUser(String username, String passwordHash, boolean mustChangePassword) {
        this(username, null, null, null, null, null, null, null, passwordHash, mustChangePassword);
    }

    public AdminUser(String username, String firstName, String lastName, String birthNumber,
            String email, String street, String city, String postalCode, String passwordHash,
            boolean mustChangePassword) {
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthNumber = birthNumber;
        this.email = email;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
        this.passwordHash = passwordHash;
        this.mustChangePassword = mustChangePassword;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public boolean mustChangePassword() {
        return mustChangePassword;
    }

    public String getBirthNumber() {
        return birthNumber;
    }

    public String getStreet() {
        return street;
    }

    public String getCity() {
        return city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void updateProfile(String firstName, String lastName, String birthNumber, String email,
            String street, String city, String postalCode) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthNumber = birthNumber;
        this.email = email;
        this.street = street;
        this.city = city;
        this.postalCode = postalCode;
    }

    public void changePassword(String passwordHash) {
        this.passwordHash = passwordHash;
        this.mustChangePassword = false;
    }
}
