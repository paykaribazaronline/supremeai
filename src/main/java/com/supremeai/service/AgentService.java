package com.supremeai.service;

import com.supremeai.dto.AgentStatus;
import com.supremeai.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String CACHE_KEY_AGENT_STATUS = "agentStatus:";
    private static final String CACHE_KEY_PROVIDER_LIST = "providerList";
    private static final long CACHE_TTL_MINUTES = 10;

    public Mono<AgentStatus> getAgentStatus(String agentId) {
        String key = CACHE_KEY_AGENT_STATUS + agentId;
        AgentStatus cached = (AgentStatus) redisTemplate.opsForValue().get(key);
        if (cached != null) {
            log.debug("Cache hit for agentId: {}", agentId);
            return Mono.just(cached);
        }

        log.debug("Cache miss, fetching from DB for agentId: {}", agentId);
        return agentRepository.findStatusById(agentId)
                .doOnNext(status -> redisTemplate.opsForValue().set(key, status, CACHE_TTL_MINUTES, TimeUnit.MINUTES));
    }

    public Mono<Void> updateAgentStatus(String agentId, AgentStatus status) {
        log.debug("Updating agent status and evicting cache for agentId: {}", agentId);
        redisTemplate.delete(CACHE_KEY_AGENT_STATUS + agentId);
        return agentRepository.updateStatus(agentId, status);
    }

    public Mono<List<Provider>> getAllProviders() {
        List<Provider> cached = (List<Provider>) redisTemplate.opsForValue().get(CACHE_KEY_PROVIDER_LIST);
        if (cached != null) {
            return Mono.just(cached);
        }
        // Since findAllProviders doesn't exist in Firestore reactive repo, return empty for now
        List<Provider> providers = List.of();
        redisTemplate.opsForValue().set(CACHE_KEY_PROVIDER_LIST, providers, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        return Mono.just(providers);
    }
}
