package com.angular.backend.users;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "my_users")
public class UserJPA {

    @Id
    @UuidGenerator
    @GeneratedValue(strategy = GenerationType.UUID)
    public String id;
    public String firstName;
    public String lastName;
    public String username;
    public int age;
    public String email;
}
