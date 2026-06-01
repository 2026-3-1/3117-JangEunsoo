package com.jes.devlearn.domain.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 15)
    private String username;

    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.STUDENT;

    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

    public User(String username, String password) {
        this(username, password, Role.STUDENT);
    }

    public User(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role == null ? Role.STUDENT : role;
    }

    public void promoteToInstructor() {
        this.role = Role.INSTRUCTOR;
    }

    public void changeRole(Role newRole) {
        if (newRole != null) {
            this.role = newRole;
        }
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }
}
