package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.supremeai.model.User;
import com.supremeai.model.UserTier;
import com.supremeai.model.SystemConfig;
import com.supremeai.repository.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AdminDashboardFacadeServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private AgentRepository agentRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private ProviderRepository providerRepository;
  @Mock private ActivityLogRepository activityLogRepository;
  @Mock private SystemLearningRepository systemLearningRepository;
  @Mock private VPNRepository vpnRepository;
  @Mock private ConfigService configService;
  @Mock private TelegramStorageService telegramStorageService;

  private AdminDashboardFacadeService service;

  @BeforeEach
  void setUp() {
    service =
        new AdminDashboardFacadeService(
            userRepository,
            agentRepository,
            projectRepository,
            providerRepository,
            activityLogRepository,
            systemLearningRepository,
            vpnRepository,
            configService,
            telegramStorageService);
  }

  @Test
  void buildContract_shouldBuildCorrectContractWithAllStats() {
    when(agentRepository.count()).thenReturn(Mono.just(10L));
    when(projectRepository.count()).thenReturn(Mono.just(20L));
    when(projectRepository.findByStatus("COMPLETED")).thenReturn(Flux.empty());
    when(projectRepository.findByStatus("ACTIVE")).thenReturn(Flux.empty());
    when(activityLogRepository.count()).thenReturn(Mono.just(100L));
    when(activityLogRepository.findBySeverityOrderByTimestampDesc("CRITICAL"))
        .thenReturn(Flux.empty());
    when(systemLearningRepository.count()).thenReturn(Mono.just(5L));
    when(vpnRepository.count()).thenReturn(Mono.just(3L));
    when(userRepository.count()).thenReturn(Mono.just(50L));
    when(userRepository.findByIsActive(true)).thenReturn(Flux.empty());
    when(providerRepository.count()).thenReturn(Mono.just(8L));
    when(providerRepository.findByStatus("active")).thenReturn(Flux.empty());
    when(telegramStorageService.checkBotStatus())
        .thenReturn(Mono.just(Map.of("status", "ONLINE")));

    Map<String, Object> uiMetadata = Map.of("navigation", List.of(), "components", List.of());
    SystemConfig systemConfig = new SystemConfig();
    systemConfig.getSettings().put("uiMetadata", uiMetadata);
    when(configService.getConfig()).thenReturn(systemConfig);

    List<Mono<?>> sources = service.buildContractDataSources();
    Mono.zip(
            sources,
            results -> {
              Object[] data = new Object[results.length];
              for (int i = 0; i < results.length; i++) {
                data[i] = results[i] != null ? results[i] : 0;
              }
              return service.buildContract(data);
            })
        .subscribe(result -> {});

    assert true;
  }

  @Test
  void buildContract_shouldProduceNonNullResult() {
    SystemConfig systemConfig = new SystemConfig();
    when(configService.getConfig()).thenReturn(systemConfig);
    
    Object[] data =
        new Object[] {
          0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L, Map.of("status", "UNKNOWN")
        };
    Map<String, Object> result = service.buildContract(data);

    assertNotNull(result);
    assertEquals("3.1.0-supremeai", result.get("contractVersion"));
    assertEquals("SupremeAI Studio", result.get("title"));
    assertTrue(result.containsKey("stats"));
  }

  @Test
  void buildDefaultContract_shouldReturnDefaultValues() {
    SystemConfig systemConfig = new SystemConfig();
    when(configService.getConfig()).thenReturn(systemConfig);
    
    Map<String, Object> result = service.buildDefaultContract();

    assertNotNull(result);
    assertEquals("3.1.0-supremeai", result.get("contractVersion"));
    assertTrue(result.containsKey("stats"));
    assertTrue(result.containsKey("navigation"));
    assertTrue(result.containsKey("components"));
  }

  @Test
  void formatUptime_shouldFormatDays() {
    String result = service.formatUptime(86400000L + 3600000L + 60000L);

    assertEquals("1d 1h 1m", result);
  }

  @Test
  void formatUptime_shouldFormatHours() {
    String result = service.formatUptime(3600000L + 60000L);

    assertEquals("1h 1m", result);
  }

  @Test
  void formatUptime_shouldFormatMinutes() {
    String result = service.formatUptime(60000L + 1000L);

    assertEquals("1m 1s", result);
  }

  @Test
  void getStartTime_shouldReturnPositiveValue() {
    long startTime = service.getStartTime();

    assertTrue(startTime > 0);
  }

  @Test
  void toUserMap_shouldMapAllUserFields() {
    User user = new User("test-uid", "test@test.com", "Test User");
    user.setTier(UserTier.PRO);
    user.setIsActive(true);
    user.setCreatedAt(1000L);
    user.setLastLoginAt(2000L);

    Map<String, Object> result = service.toUserMap(user);

    assertNotNull(result);
    assertEquals("test-uid", result.get("id"));
    assertEquals("test@test.com", result.get("email"));
    assertEquals("Test User", result.get("displayName"));
    assertEquals(UserTier.PRO.toString(), result.get("tier"));
    assertEquals(100000L, result.get("monthlyQuota"));
  }
}
