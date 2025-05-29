package com.krontech.worklog.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(exclude = {"teamLead", "subordinates", "worklogs", "department", "directedDepartment"})
@ToString(exclude = {"teamLead", "subordinates", "worklogs", "department", "directedDepartment"})
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "grade_id", nullable = false)
    private Grade grade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_lead_id")
    private Employee teamLead;

    @OneToMany(mappedBy = "teamLead", fetch = FetchType.LAZY)
    private List<Employee> subordinates = new ArrayList<>();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_id")
    private Department department;

    // Reverse relationship for director
    @OneToOne(mappedBy = "director", fetch = FetchType.LAZY)
    private Department directedDepartment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.EMPLOYEE;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "employee", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<Worklog> worklogs = new ArrayList<>();

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
        if (role == null) {
            role = Role.EMPLOYEE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    // Check if this employee can view another employee's data
    public boolean canViewEmployee(Employee other) {
        if (this.equals(other)) return true; // Can view own data

        // Directors can only view employees in their department
        if (this.role == Role.DIRECTOR && this.department != null && other.getDepartment() != null) {
            return this.department.getId().equals(other.getDepartment().getId());
        }

        // Team leads can view their team members
        return this.role == Role.TEAM_LEAD && other.getTeamLead() != null
                && other.getTeamLead().getId().equals(this.id);
    }

    // Check if employee is a director
    public boolean isDirector() {
        return this.role == Role.DIRECTOR && this.directedDepartment != null;
    }

    // Get all employees this person can view
    public List<Employee> getViewableEmployees() {
        List<Employee> viewable = new ArrayList<>();

        if (this.role == Role.DIRECTOR && this.department != null) {
            // Directors see all employees in their department
            return department.getEmployees().stream()
                    .filter(Employee::getIsActive)
                    .toList();
        } else if (this.role == Role.TEAM_LEAD) {
            // Team leads see only their direct reports
            return subordinates.stream()
                    .filter(Employee::getIsActive)
                    .toList();
        }

        // Regular employees see only themselves
        viewable.add(this);
        return viewable;
    }
}