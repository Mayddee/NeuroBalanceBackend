package org.example.ainote.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Note Entity
 * ✅ FIXED: Added @GeneratedValue for auto-increment ID
 */
@Entity
@Data
@Table(name = "notes")
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "created", nullable = false, updatable = false)
    private LocalDateTime created;

    @PrePersist
    public void prePersist() {
        this.created = LocalDateTime.now();
    }
}