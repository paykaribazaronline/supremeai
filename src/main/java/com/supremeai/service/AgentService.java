package com.supremeai.service;

import com.supremeai.dto.AgentStatus;
import com.supremeai.repository.AgentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AgentService {

    private final AgentRepository agentRepository;

    @Cacheable(value = "agentStatus", key = "#agentId")
    public AgentStatus getAgentStatus(String agentId) {
        log.debug("Fetching agent status from DB for agentId: {}", agentId);
        return agentRepository.findStatusById(agentId);
    }

    @CacheEvict(value = "agentStatus", key = "#agentId")
    public void updateAgentStatus(String agentId, AgentStatus status) {
        log.debug("Updating agent status and evicting cache for agentId: {}", agentId);
        agentRepository.updateStatus(agentId, status);
    }

    @Cacheable(value = "providerList", unless = "#result == null")
    public List<Provider> getAllProviders() {
        log.debug("Fetching all providers from DB");
        return agentRepository.findAllProviders();
    }
}
