package com.krontech.worklog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"employees", "director"})
@ToString(exclude = {"employees", "director"})
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "director_id")
    private Employee director;

    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<Employee> employees = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper method to get all team leads in this department
    public List<Employee> getTeamLeads() {
        return employees.stream()
                .filter(e -> e.getRole() == Role.TEAM_LEAD && e.getIsActive())
                .toList();
    }

    // Check if an employee is the director of this department
    public boolean isDirector(Employee employee) {
        return director != null && director.getId().equals(employee.getId());
    }
}