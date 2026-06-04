package com.supremeai.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void testValidation_validRequest() {
        AuthenticationController.RegisterRequest request = new AuthenticationController.RegisterRequest(
            "test@example.com", "Password123", "Test User"
        );
        Set<ConstraintViolation<AuthenticationController.RegisterRequest>> violations = validator.validate(request);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testValidation_emptyEmail() {
        AuthenticationController.RegisterRequest request = new AuthenticationController.RegisterRequest(
            "", "Password123", "Test User"
        );
        Set<ConstraintViolation<AuthenticationController.RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Empty email should trigger validation error");
    }

    @Test
    void testValidation_invalidEmail() {
        AuthenticationController.RegisterRequest request = new AuthenticationController.RegisterRequest(
            "invalid-email", "Password123", "Test User"
        );
        Set<ConstraintViolation<AuthenticationController.RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Invalid email format should trigger validation error");
    }

    @Test
    void testValidation_shortPassword() {
        AuthenticationController.RegisterRequest request = new AuthenticationController.RegisterRequest(
            "test@example.com", "short", "Test User"
        );
        Set<ConstraintViolation<AuthenticationController.RegisterRequest>> violations = validator.validate(request);
        assertFalse(violations.isEmpty(), "Short password should trigger validation error");
    }
}
