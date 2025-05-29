package com.krontech.worklog.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "worklogs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_date_type",
                        columnNames = {"employee_id", "work_date", "worklog_type_id"} // Prevent duplicates
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder // Lombok: generates builder pattern
@EqualsAndHashCode(exclude = {"employee", "worklogType"})
@ToString(exclude = {"employee", "worklogType"})
public class Worklog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // optional=false means NOT NULL
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.EAGER) // Always load worklog type
    @JoinColumn(name = "worklog_type_id", nullable = false)
    private WorklogType worklogType;

    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    @Column(name = "hours_worked", nullable = false)
    @Min(value = 1, message = "Minimum hours is 1")
    @Max(value = 8, message = "Maximum hours is 8")
    private Integer hoursWorked;

    @Column(columnDefinition = "TEXT") // For PostgreSQL TEXT type
    private String description;

    @Column(name = "project_name", length = 200)
    private String projectName;

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

    // Helper method to convert hours to days
    public double getWorkDays() {
        return hoursWorked / 8.0;
    }

    // Check if worklog is editable (e.g., within last 7 days)
    public boolean isEditable() {
        return workDate.isAfter(LocalDate.now().minusDays(7));
    }
}