package com.krontech.worklog.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name="grades")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Grade {

    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Uses database auto-increment
    private Integer id;

    @Column(name = "grade_level", nullable = false, unique = true)
    private Integer gradeLevel;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "created_at", updatable = false) // Never update after insert
    private LocalDateTime createdAt;

    @PrePersist // Called before entity is saved for first time
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
