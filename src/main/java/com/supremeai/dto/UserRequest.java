package com.supremeai.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRequest {
    private String description;
    private LanguagePreference languagePreference;
    private String userId;
}
