package com.supremeai.dto;

import com.supremeai.dto.ApiKeyBulkRequest;
import com.supremeai.dto.ApiKeyTestRequestBody;
import com.supremeai.dto.ApiKeyUpdateRequest;
import com.supremeai.dto.ApiKeyUsageRequest;
import com.supremeai.dto.valid.ApiKeyCreateDTO;
import com.supremeai.dto.valid.UserCreateDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive validation tests for DTOs with Bean Validation constraints.
 * Covers @NotNull, @Size, @Email, @Pattern, @NotEmpty, etc.
 *
 * Currently tests 25+ validation scenarios across 12 DTO types.
 */
class DtoValidationTest {

    private final Validator validator;

    public DtoValidationTest() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        this.validator = factory.getValidator();
    }

    // ==================== UserCreateDTO Tests (valid package) ====================

    @Test
    void testUserCreateDTO_ValidInput() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("Password123");
        dto.setDisplayName("Test User");

        Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid UserCreateDTO should have no violations");
    }

    @Test
    void testUserCreateDTO_InvalidEmail() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("invalid-email");
        dto.setPassword("Password123");
        dto.setDisplayName("Test User");

        Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void testUserCreateDTO_BlankEmail() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("");
        dto.setPassword("Password123");
        dto.setDisplayName("Test User");

        Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testUserCreateDTO_NullEmail() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setPassword("Password123");
        dto.setDisplayName("Test User");

        Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testUserCreateDTO_ShortPassword() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("Pass"); // Too short: < 8
        dto.setDisplayName("Test User");

        Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"password123", "PASSWORD123", "PasswordABC"})
    void testUserCreateDTO_WeakPassword(String weakPassword) {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("test@example.com");
        dto.setPassword(weakPassword);
        dto.setDisplayName("Test User");

        Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testUserCreateDTO_LongDisplayName() {
        UserCreateDTO dto = new UserCreateDTO();
        dto.setEmail("test@example.com");
        dto.setPassword("Password123");
        dto.setDisplayName("A".repeat(101)); // > 100 max

        Set<ConstraintViolation<UserCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    // ==================== ApiKeyCreateDTO Tests (valid package) ====================

    @Test
    void testApiKeyCreateDTO_Valid() {
        ApiKeyCreateDTO dto = new ApiKeyCreateDTO();
        dto.setProvider("openai");
        dto.setLabel("My API Key");
        dto.setApiKey("sk-1234567890abcdefghijklmnop");
        dto.setBaseUrl("https://api.openai.com/v1");
        dto.setModels(Stream.of("gpt-4", "gpt-3.5-turbo").collect(Collectors.toList()));

        Set<ConstraintViolation<ApiKeyCreateDTO>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testApiKeyCreateDTO_BlankProvider() {
        ApiKeyCreateDTO dto = new ApiKeyCreateDTO();
        dto.setProvider("");
        dto.setLabel("My API Key");
        dto.setApiKey("sk-1234567890");

        Set<ConstraintViolation<ApiKeyCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyCreateDTO_NullProvider() {
        ApiKeyCreateDTO dto = new ApiKeyCreateDTO();
        dto.setLabel("Test");
        dto.setApiKey("sk-12345");

        Set<ConstraintViolation<ApiKeyCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyCreateDTO_ShortApiKey() {
        ApiKeyCreateDTO dto = new ApiKeyCreateDTO();
        dto.setProvider("openai");
        dto.setLabel("Test");
        dto.setApiKey("sk-12"); // < 10 min

        Set<ConstraintViolation<ApiKeyCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyCreateDTO_LongBaseUrl() {
        ApiKeyCreateDTO dto = new ApiKeyCreateDTO();
        dto.setProvider("openai");
        dto.setLabel("Test");
        dto.setApiKey("sk-12345");
        dto.setBaseUrl("https://" + "a".repeat(500)); // > 500 max

        Set<ConstraintViolation<ApiKeyCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyCreateDTO_InvalidBaseUrlPattern() {
        ApiKeyCreateDTO dto = new ApiKeyCreateDTO();
        dto.setProvider("openai");
        dto.setLabel("Test");
        dto.setApiKey("sk-12345");
        dto.setBaseUrl("not-a-url"); // Invalid pattern

        Set<ConstraintViolation<ApiKeyCreateDTO>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    // ==================== TranslationRequest Tests ====================

    @Test
    void testTranslationRequest_Valid() {
        TranslationRequest dto = new TranslationRequest();
        dto.setText("Hello, world!");
        dto.setFromLanguage("en");
        dto.setToLanguage("es");

        Set<ConstraintViolation<TranslationRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid TranslationRequest should have no violations");
    }

    @Test
    void testTranslationRequest_BlankText() {
        TranslationRequest dto = new TranslationRequest();
        dto.setText("");
        dto.setFromLanguage("en");
        dto.setToLanguage("es");

        Set<ConstraintViolation<TranslationRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testTranslationRequest_LongText() {
        TranslationRequest dto = new TranslationRequest();
        dto.setText("a".repeat(10001)); // > 10000 max
        dto.setFromLanguage("en");
        dto.setToLanguage("es");

        Set<ConstraintViolation<TranslationRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({
        "en, fr",
        "es, de",
        "zh, ja",
        "bn, en"
    })
    void testTranslationRequest_ValidLanguageCodes(String from, String to) {
        TranslationRequest dto = new TranslationRequest();
        dto.setText("Test");
        dto.setFromLanguage(from);
        dto.setToLanguage(to);

        Set<ConstraintViolation<TranslationRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "abcdefghijk", "abcdefghijkl"}) // <2 or >10 chars
    void testTranslationRequest_InvalidFromLanguage_Size(String lang) {
        TranslationRequest dto = new TranslationRequest();
        dto.setText("Test");
        dto.setFromLanguage(lang);
        dto.setToLanguage("es");

        Set<ConstraintViolation<TranslationRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty(), "Language code '" + lang + "' should violate size constraint");
    }

    // ==================== ProjectCreateRequest Tests ====================

    @Test
    void testProjectCreateRequest_Valid() {
        ProjectCreateRequest dto = new ProjectCreateRequest();
        dto.setName("My Project");
        dto.setDescription("A test project");

        Set<ConstraintViolation<ProjectCreateRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testProjectCreateRequest_BlankName() {
        ProjectCreateRequest dto = new ProjectCreateRequest();
        dto.setName("");
        dto.setDescription("Test");

        Set<ConstraintViolation<ProjectCreateRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testProjectCreateRequest_NullName() {
        ProjectCreateRequest dto = new ProjectCreateRequest();
        dto.setDescription("Test");

        Set<ConstraintViolation<ProjectCreateRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testProjectCreateRequest_LongDescription() {
        ProjectCreateRequest dto = new ProjectCreateRequest();
        dto.setName("Test");
        dto.setDescription("a".repeat(2001)); // > 2000

        Set<ConstraintViolation<ProjectCreateRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    // ==================== UserRequest Tests ====================

    @Test
    void testUserRequest_Valid() {
        UserRequest dto = new UserRequest();
        dto.setDescription("A test user");
        dto.setLanguagePreference(LanguagePreference.ENGLISH);
        dto.setUserId("user123");

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testUserRequest_BlankDescription() {
        UserRequest dto = new UserRequest();
        dto.setDescription("");
        dto.setLanguagePreference(LanguagePreference.ENGLISH);
        dto.setUserId("user123");

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testUserRequest_NullLanguagePreference() {
        UserRequest dto = new UserRequest();
        dto.setDescription("Test");
        dto.setUserId("user123");
        dto.setLanguagePreference(null);

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testUserRequest_LongUserId() {
        UserRequest dto = new UserRequest();
        dto.setDescription("Test");
        dto.setLanguagePreference(LanguagePreference.ENGLISH);
        dto.setUserId("a".repeat(256)); // > 255

        Set<ConstraintViolation<UserRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    // ==================== ApiKeyRequest Tests ====================

    @Test
    void testApiKeyRequest_Valid() {
        ApiKeyRequest dto = new ApiKeyRequest();
        dto.setProvider("anthropic");
        dto.setApiKey("sk-ant-1234567890");

        Set<ConstraintViolation<ApiKeyRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testApiKeyRequest_BlankProvider() {
        ApiKeyRequest dto = new ApiKeyRequest();
        dto.setProvider("");
        dto.setApiKey("sk-ant-12345");

        Set<ConstraintViolation<ApiKeyRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyRequest_BlankApiKey() {
        ApiKeyRequest dto = new ApiKeyRequest();
        dto.setProvider("openai");
        dto.setApiKey("");

        Set<ConstraintViolation<ApiKeyRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    // ==================== ApiKeyBulkUpdateRequest Tests ====================

    @Test
    void testApiKeyBulkUpdateRequest_Valid() {
        ApiKeyBulkUpdateRequest dto = new ApiKeyBulkUpdateRequest();
        dto.setKeyIds(Stream.of("key1", "key2", "key3").collect(Collectors.toList()));
        dto.setProvider("openai");
        dto.setLabel("Updated Label");

        Set<ConstraintViolation<ApiKeyBulkUpdateRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testApiKeyBulkUpdateRequest_NullKeyIds() {
        ApiKeyBulkUpdateRequest dto = new ApiKeyBulkUpdateRequest();
        dto.setProvider("openai");

        Set<ConstraintViolation<ApiKeyBulkUpdateRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyBulkUpdateRequest_EmptyKeyIds() {
        ApiKeyBulkUpdateRequest dto = new ApiKeyBulkUpdateRequest();
        dto.setKeyIds(Collections.emptyList());
        dto.setProvider("openai");

        Set<ConstraintViolation<ApiKeyBulkUpdateRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyBulkUpdateRequest_LargeKeyIds() {
        ApiKeyBulkUpdateRequest dto = new ApiKeyBulkUpdateRequest();
        dto.setKeyIds(Stream.iterate(1, i -> i + 1)
            .limit(101)
            .map(Object::toString)
            .collect(Collectors.toList()));
        dto.setProvider("openai");

        Set<ConstraintViolation<ApiKeyBulkUpdateRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    // ==================== Bulk Delete/Regenerate Tests ====================

    @Test
    void testBulkDeleteRequest_Valid() {
        BulkDeleteRequest dto = new BulkDeleteRequest();
        dto.setKeyIds(Stream.of("id1", "id2").collect(Collectors.toList()));

        Set<ConstraintViolation<BulkDeleteRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testBulkDeleteRequest_Empty() {
        BulkDeleteRequest dto = new BulkDeleteRequest();
        dto.setKeyIds(Collections.emptyList());

        Set<ConstraintViolation<BulkDeleteRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testBulkRegenerateRequest_Valid() {
        BulkRegenerateRequest dto = new BulkRegenerateRequest();
        dto.setKeyIds(Stream.of("id1", "id2").collect(Collectors.toList()));

        Set<ConstraintViolation<BulkRegenerateRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    // ==================== ApiKeyBulk*Request Tests ====================

    @Test
    void testApiKeyBulkTestRequest_Valid() {
        ApiKeyBulkTestRequest dto = new ApiKeyBulkTestRequest();
        dto.setKeyIds(Stream.of("key1").collect(Collectors.toList()));

        Set<ConstraintViolation<ApiKeyBulkTestRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testApiKeyBulkTestRequest_NullKeyIds() {
        ApiKeyBulkTestRequest dto = new ApiKeyBulkTestRequest();

        Set<ConstraintViolation<ApiKeyBulkTestRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyBulkDeleteRequest_Valid() {
        ApiKeyBulkDeleteRequest dto = new ApiKeyBulkDeleteRequest();
        dto.setKeyIds(Stream.of("key1", "key2").collect(Collectors.toList()));

        Set<ConstraintViolation<ApiKeyBulkDeleteRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testApiKeyBulkRegenerateRequest_Valid() {
        ApiKeyBulkRegenerateRequest dto = new ApiKeyBulkRegenerateRequest();
        dto.setKeyIds(Stream.of("key1").collect(Collectors.toList()));

        Set<ConstraintViolation<ApiKeyBulkRegenerateRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    // ==================== ApiKeyUpdateRequestBody Tests ====================

    @Test
    void testApiKeyUpdateRequestBody_Valid() {
        ApiKeyUpdateRequestBody dto = new ApiKeyUpdateRequestBody();
        dto.setProvider("openai");
        dto.setLabel("New Label");
        dto.setBaseUrl("https://api.openai.com/v1");

        Set<ConstraintViolation<ApiKeyUpdateRequestBody>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testApiKeyUpdateRequestBody_AllOptional() {
        ApiKeyUpdateRequestBody dto = new ApiKeyUpdateRequestBody();
        // All fields optional, empty DTO should be valid

        Set<ConstraintViolation<ApiKeyUpdateRequestBody>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    // ==================== ApiKeyCreateRequest Tests ====================

    @Test
    void testApiKeyCreateRequest_Valid() {
        ApiKeyCreateRequest dto = new ApiKeyCreateRequest();
        dto.setProvider("openai");
        dto.setLabel("Test Key");
        dto.setApiKey("sk-1234567890");

        Set<ConstraintViolation<ApiKeyCreateRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testApiKeyCreateRequest_MissingAll() {
        ApiKeyCreateRequest dto = new ApiKeyCreateRequest();

        Set<ConstraintViolation<ApiKeyCreateRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        // Should have violations for provider, label, apiKey (all @NotBlank)
        assertTrue(violations.size() >= 3);
    }

    // ==================== ApiKeyTestRequestBody Tests ====================

    @Test
    void testApiKeyTestRequestBody_Valid() {
        ApiKeyTestRequestBody dto = new ApiKeyTestRequestBody();
        dto.setName("openai");
        dto.setApiKey("sk-1234567890");

        Set<ConstraintViolation<ApiKeyTestRequestBody>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid ApiKeyTestRequestBody should have no violations");
    }

    @Test
    void testApiKeyTestRequestBody_BlankName() {
        ApiKeyTestRequestBody dto = new ApiKeyTestRequestBody();
        dto.setName("");
        dto.setApiKey("sk-12345");

        Set<ConstraintViolation<ApiKeyTestRequestBody>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("name")));
    }

    @Test
    void testApiKeyTestRequestBody_NullName() {
        ApiKeyTestRequestBody dto = new ApiKeyTestRequestBody();
        dto.setApiKey("sk-12345");

        Set<ConstraintViolation<ApiKeyTestRequestBody>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyTestRequestBody_BlankApiKey() {
        ApiKeyTestRequestBody dto = new ApiKeyTestRequestBody();
        dto.setName("openai");
        dto.setApiKey("");

        Set<ConstraintViolation<ApiKeyTestRequestBody>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("apiKey")));
    }

    @Test
    void testApiKeyTestRequestBody_NullApiKey() {
        ApiKeyTestRequestBody dto = new ApiKeyTestRequestBody();
        dto.setName("openai");

        Set<ConstraintViolation<ApiKeyTestRequestBody>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    // ==================== ApiKeyBulkRequest Tests ====================

    @Test
    void testApiKeyBulkRequest_Valid() {
        ApiKeyBulkRequest dto = new ApiKeyBulkRequest();
        dto.setKeyIds(Stream.of("key1", "key2", "key3").collect(Collectors.toList()));

        Set<ConstraintViolation<ApiKeyBulkRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid ApiKeyBulkRequest should have no violations");
    }

    @Test
    void testApiKeyBulkRequest_NullKeyIds() {
        ApiKeyBulkRequest dto = new ApiKeyBulkRequest();
        dto.setKeyIds(null);

        Set<ConstraintViolation<ApiKeyBulkRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("keyIds")));
    }

    @Test
    void testApiKeyBulkRequest_EmptyKeyIds() {
        ApiKeyBulkRequest dto = new ApiKeyBulkRequest();
        dto.setKeyIds(Collections.emptyList());

        Set<ConstraintViolation<ApiKeyBulkRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyBulkRequest_LargeKeyIds() {
        ApiKeyBulkRequest dto = new ApiKeyBulkRequest();
        dto.setKeyIds(Stream.iterate(1, i -> i + 1)
            .limit(101)
            .map(Object::toString)
            .collect(Collectors.toList()));

        Set<ConstraintViolation<ApiKeyBulkRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    // ==================== ApiKeyUpdateRequest Tests ====================

    @Test
    void testApiKeyUpdateRequest_Valid() {
        ApiKeyUpdateRequest dto = new ApiKeyUpdateRequest();
        dto.setProvider("openai");
        dto.setLabel("Updated Label");
        dto.setBaseUrl("https://api.openai.com/v1");

        Set<ConstraintViolation<ApiKeyUpdateRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid ApiKeyUpdateRequest should have no violations");
    }

    @Test
    void testApiKeyUpdateRequest_AllOptional() {
        ApiKeyUpdateRequest dto = new ApiKeyUpdateRequest();
        // All fields are optional, empty DTO should be valid

        Set<ConstraintViolation<ApiKeyUpdateRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty());
    }

    @Test
    void testApiKeyUpdateRequest_TooLongProvider() {
        ApiKeyUpdateRequest dto = new ApiKeyUpdateRequest();
        dto.setProvider("a".repeat(51)); // > 50 max
        dto.setLabel("Test");

        Set<ConstraintViolation<ApiKeyUpdateRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("provider")));
    }

    @Test
    void testApiKeyUpdateRequest_TooLongLabel() {
        ApiKeyUpdateRequest dto = new ApiKeyUpdateRequest();
        dto.setProvider("openai");
        dto.setLabel("b".repeat(101)); // > 100 max

        Set<ConstraintViolation<ApiKeyUpdateRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("label")));
    }

    @Test
    void testApiKeyUpdateRequest_TooLongBaseUrl() {
        ApiKeyUpdateRequest dto = new ApiKeyUpdateRequest();
        dto.setProvider("openai");
        dto.setBaseUrl("https://" + "c".repeat(500)); // > 500 max

        Set<ConstraintViolation<ApiKeyUpdateRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("baseUrl")));
    }

    // ==================== ApiKeyUsageRequest Tests ====================

    @Test
    void testApiKeyUsageRequest_Valid() {
        ApiKeyUsageRequest dto = new ApiKeyUsageRequest();
        dto.setStartDate("2025-01-01");
        dto.setEndDate("2025-12-31");
        dto.setProvider("openai");

        Set<ConstraintViolation<ApiKeyUsageRequest>> violations = validator.validate(dto);
        assertTrue(violations.isEmpty(), "Valid ApiKeyUsageRequest should have no violations");
    }

    @Test
    void testApiKeyUsageRequest_BlankStartDate() {
        ApiKeyUsageRequest dto = new ApiKeyUsageRequest();
        dto.setStartDate("");
        dto.setEndDate("2025-12-31");
        dto.setProvider("openai");

        Set<ConstraintViolation<ApiKeyUsageRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("startDate")));
    }

    @Test
    void testApiKeyUsageRequest_NullStartDate() {
        ApiKeyUsageRequest dto = new ApiKeyUsageRequest();
        dto.setEndDate("2025-12-31");
        dto.setProvider("openai");

        Set<ConstraintViolation<ApiKeyUsageRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyUsageRequest_BlankEndDate() {
        ApiKeyUsageRequest dto = new ApiKeyUsageRequest();
        dto.setStartDate("2025-01-01");
        dto.setEndDate("");
        dto.setProvider("openai");

        Set<ConstraintViolation<ApiKeyUsageRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyUsageRequest_BlankProvider() {
        ApiKeyUsageRequest dto = new ApiKeyUsageRequest();
        dto.setStartDate("2025-01-01");
        dto.setEndDate("2025-12-31");
        dto.setProvider("");

        Set<ConstraintViolation<ApiKeyUsageRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        assertTrue(violations.stream()
            .anyMatch(v -> v.getPropertyPath().toString().equals("provider")));
    }

    @Test
    void testApiKeyUsageRequest_NullProvider() {
        ApiKeyUsageRequest dto = new ApiKeyUsageRequest();
        dto.setStartDate("2025-01-01");
        dto.setEndDate("2025-12-31");

        Set<ConstraintViolation<ApiKeyUsageRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
    }

    @Test
    void testApiKeyUsageRequest_MissingAll() {
        ApiKeyUsageRequest dto = new ApiKeyUsageRequest();

        Set<ConstraintViolation<ApiKeyUsageRequest>> violations = validator.validate(dto);
        assertFalse(violations.isEmpty());
        // Should have violations for startDate, endDate, provider (all @NotBlank)
        assertEquals(3, violations.size());
    }
}
