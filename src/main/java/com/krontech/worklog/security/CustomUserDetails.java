package com.krontech.worklog.security;

import com.krontech.worklog.entity.Employee;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * @param employee Getter to access the underlying Employee
 */
public record CustomUserDetails(Employee employee) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Convert role to Spring Security authority (add ROLE_ prefix)
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + employee.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return employee.getPassword();
    }

    @Override
    public String getUsername() {
        return employee.getEmail(); // Using email as username
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return employee.getIsActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return employee.getIsActive();
    }

    public Integer getId() {
        return employee.getId();
    }

    public String getFullName() {
        return employee.getFullName();
    }
}