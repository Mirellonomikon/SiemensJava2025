package com.siemens.internship.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ItemTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void validEmail_noViolations() {
        Item item = new Item();
        item.setName("Test Item");
        item.setEmail("test@example.com");

        Set<ConstraintViolation<Item>> violations = validator.validate(item);

        assertTrue(violations.isEmpty());
    }

    @Test
    void invalidEmail_hasViolations() {
        Item item = new Item();
        item.setName("Test Item");
        item.setEmail("not an email");

        Set<ConstraintViolation<Item>> violations = validator.validate(item);

        assertFalse(violations.isEmpty());
        boolean hasEmailViolation = violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
        assertTrue(hasEmailViolation);
    }

    @Test
    void gettersAndSetters() {
        Item item = new Item();

        item.setId(1L);
        item.setName("Test Item");
        item.setDescription("Test Description");
        item.setStatus("NEW");
        item.setEmail("test@example.com");

        assertEquals(1L, item.getId());
        assertEquals("Test Item", item.getName());
        assertEquals("Test Description", item.getDescription());
        assertEquals("NEW", item.getStatus());
        assertEquals("test@example.com", item.getEmail());
    }

    @Test
    void allArgsConstructor() {
        Item item = new Item(2L, "Created Item", "Created Description", "CREATED", "created@example.com");

        assertEquals(2L, item.getId());
        assertEquals("Created Item", item.getName());
        assertEquals("Created Description", item.getDescription());
        assertEquals("CREATED", item.getStatus());
        assertEquals("created@example.com", item.getEmail());
    }

    @Test
    void noArgsConstructor() {
        Item item = new Item();

        assertNull(item.getId());
        assertNull(item.getName());
        assertNull(item.getDescription());
        assertNull(item.getStatus());
        assertNull(item.getEmail());
    }
}