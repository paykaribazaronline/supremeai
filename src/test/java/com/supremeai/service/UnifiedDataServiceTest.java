package com.supremeai.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnifiedDataServiceTest {

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter dataCollectCounter;

    @Mock
    private Counter dataPurgeCounter;

    @Mock
    private Counter dataQueryCounter;

    private UnifiedDataService unifiedDataService;

    @BeforeEach
    void setUp() {
        when(meterRegistry.counter("unified_data_service.collect")).thenReturn(dataCollectCounter);
        when(meterRegistry.counter("unified_data_service.purge")).thenReturn(dataPurgeCounter);
        when(meterRegistry.counter("unified_data_service.query")).thenReturn(dataQueryCounter);
        unifiedDataService = new UnifiedDataService(meterRegistry);
    }

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