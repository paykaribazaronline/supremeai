package com.supremeai.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedDataServiceTest {

    @InjectMocks
    private UnifiedDataService unifiedDataService;

    @Test
    void collectData_ShouldNotThrowException() {
        // GIVEN
        String source = "testSource";
        Object data = new Object();

        // WHEN & THEN
        assertDoesNotThrow(() -> unifiedDataService.collectData(source, data));
    }

    @Test
    void purgeOldData_ShouldNotThrowException() {
        // WHEN & THEN
        assertDoesNotThrow(() -> unifiedDataService.purgeOldData());
    }

    @Test
    void getCollectedData_ShouldReturnNull() {
        // GIVEN
        String query = "testQuery";

        // WHEN
        Object result = unifiedDataService.getCollectedData(query);

        // THEN
        assertNull(result);
    }
}