package com.krontech.worklog.controller;

import com.krontech.worklog.dto.request.LoginRequest;
import com.krontech.worklog.dto.response.LoginResponse;
import com.krontech.worklog.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest,
                                   HttpServletRequest request) {
        try {
            // Create authentication token
            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    );

            // Authenticate
            Authentication authentication = authenticationManager.authenticate(authToken);

            // Create new session
            HttpSession session = request.getSession(true);

            // Set authentication in SecurityContext
            SecurityContext securityContext = SecurityContextHolder.getContext();
            securityContext.setAuthentication(authentication);

            // Save SecurityContext to session
            session.setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    securityContext
            );

            // Get user details
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

            // Build response
            LoginResponse response = LoginResponse.builder()
                    .id(userDetails.employee().getId())
                    .email(userDetails.employee().getEmail())
                    .firstName(userDetails.employee().getFirstName())
                    .lastName(userDetails.employee().getLastName())
                    .role(userDetails.employee().getRole().name())
                    .departmentId(userDetails.employee().getDepartment().getId())
                    .departmentName(userDetails.employee().getDepartment().getName())
                    .build();

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid email or password");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        LoginResponse response = LoginResponse.builder()
                .id(userDetails.employee().getId())
                .email(userDetails.employee().getEmail())
                .firstName(userDetails.employee().getFirstName())
                .lastName(userDetails.employee().getLastName())
                .role(userDetails.employee().getRole().name())
                .departmentId(userDetails.employee().getDepartment().getId())
                .departmentName(userDetails.employee().getDepartment().getName())
                .build();

        return ResponseEntity.ok(response);
    }
}