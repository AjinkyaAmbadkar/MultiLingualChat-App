package com.multilingual.chat.app.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "preferred_language", nullable = false)
    private String preferredLanguage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public User() {

    }

    public User(Long id, String name, String email, String prefferedLanguage, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.preferredLanguage = prefferedLanguage;
        this.createdAt = createdAt;
    }

    @PrePersist
    public void PrePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // getters
    public Long getId() {
        return id;
    }

    public String getname() {
        return name;
    }

    public String getemail() {
        return email;
    }

    public String getPreferredLanguage() {
        return preferredLanguage;
    }

    public LocalDateTime getcreatedAt() {
        return createdAt;
    }

    // setters
    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPreferredLanguage(String preferredLanguage) {
        this.preferredLanguage = preferredLanguage;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
