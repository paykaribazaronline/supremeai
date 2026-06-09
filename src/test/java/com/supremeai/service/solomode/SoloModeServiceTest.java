package com.supremeai.service.solomode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.supremeai.service.SoloModeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class SoloModeServiceTest {

  @Mock private WebClient.Builder webClientBuilder;

  private SoloModeService soloModeService;

  @BeforeEach
  void setUp() {
    soloModeService = new SoloModeService(webClientBuilder);
  }

  @Test
  void testGetSystemRamBytes() {
    long ram = soloModeService.getSystemRamBytes();
    assertTrue(ram > 0);
  }

  @Test
  void testGetCpuCoresCount() {
    int cores = soloModeService.getCpuCoresCount();
    assertTrue(cores > 0);
  }

  @Test
  void testSelectOllamaModel() {
    String model = soloModeService.selectOllamaModel();
    assertNotNull(model);
    assertTrue(model.equals("llama3:8b") || model.equals("phi3:mini") || model.equals("tinyllama"));
  }
}
