
package com.supremeai.service;

import com.supremeai.dto.ProjectDTO;
import com.supremeai.dto.UserApiKeyDTO;
import com.supremeai.dto.UserDTO;
import com.supremeai.model.ExistingProject;
import com.supremeai.model.User;
import com.supremeai.model.UserApiKey;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for mapping entities to DTOs.
 * Optimizes data transfer by only including necessary fields.
 */
@Service
public class DTOMapperService {

    /**
     * Convert User entity to UserDTO
     */
    public UserDTO toUserDTO(User user) {
        if (user == null) {
            return null;
        }

        return new UserDTO(
            user.getFirebaseUid(),
            user.getEmail(),
            user.getDisplayName(),
            user.getTier() != null ? user.getTier().toString() : "FREE",
            user.fetchMonthlyQuota(),
            user.getCurrentUsage(),
            user.getCreatedAt() != null ? java.time.LocalDateTime.parse(user.getCreatedAt()) : null,
            user.getLastLoginAt() != null ? java.time.LocalDateTime.parse(user.getLastLoginAt()) : null,
            user.getIsActive()
        );
    }

    /**
     * Convert List of User entities to List of UserDTOs
     */
    public List<UserDTO> toUserDTOList(List<User> users) {
        if (users == null) {
            return List.of();
        }
        return users.stream()
            .map(this::toUserDTO)
            .collect(Collectors.toList());
    }

    /**
     * Convert UserApiKey entity to UserApiKeyDTO
     */
    public UserApiKeyDTO toUserApiKeyDTO(UserApiKey apiKey) {
        if (apiKey == null) {
            return null;
        }

        UserApiKeyDTO dto = new UserApiKeyDTO();
        dto.setId(apiKey.getId());
        dto.setUserId(apiKey.getUserId());
        dto.setProvider(apiKey.getProvider());
        dto.setLabel(apiKey.getLabel());
        dto.setMaskedKey(apiKey.getMaskedKey());
        dto.setBaseUrl(apiKey.getBaseUrl());
        dto.setModels(apiKey.getModels());
        dto.setStatus(apiKey.getStatus());
        dto.setRequestCount(apiKey.getRequestCount());
        dto.setEstimatedCost(apiKey.getEstimatedCost());
        dto.setAddedAt(apiKey.getAddedAt());
        dto.setLastTested(apiKey.getLastTested());
        dto.setLastUsed(apiKey.getLastUsed());
        dto.setRotationDueAt(apiKey.getRotationDueAt());
        dto.setNeedsRotation(apiKey.needsRotation());

        return dto;
    }

    /**
     * Convert List of UserApiKey entities to List of UserApiKeyDTOs
     */
    public List<UserApiKeyDTO> toUserApiKeyDTOList(List<UserApiKey> apiKeys) {
        if (apiKeys == null) {
            return List.of();
        }
        return apiKeys.stream()
            .map(this::toUserApiKeyDTO)
            .collect(Collectors.toList());
    }

    /**
     * Convert ExistingProject entity to ProjectDTO
     */
    public ProjectDTO toProjectDTO(ExistingProject project) {
        if (project == null) {
            return null;
        }

        return new ProjectDTO(
            project.getId(),
            project.getName(),
            project.getDescription(),
            project.getStatus(),
            project.getType(),
            project.getOwnerId(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }

    /**
     * Convert List of ExistingProject entities to List of ProjectDTOs
     */
    public List<ProjectDTO> toProjectDTOList(List<ExistingProject> projects) {
        if (projects == null) {
            return List.of();
        }
        return projects.stream()
            .map(this::toProjectDTO)
            .collect(Collectors.toList());
    }
}
