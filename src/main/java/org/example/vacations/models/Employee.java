package org.example.vacations.models;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder @ToString(exclude = "accounts")
@Entity @Table(name = "employees")
public class Employee {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Builder.Default
    @OneToMany(mappedBy = "owner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts = new ArrayList<>();

    // in Employee.java
    @Column(nullable = false)
    private String password;   // bcrypt in DB column `password`

    @Column(nullable = false)
    private String role;       // "ADMIN" or "USER" (no ROLE_ prefix)

    @Column(nullable = false)
    private Boolean enabled;   // 1/0 in DB
         // "ADMIN" or "USER"


}
