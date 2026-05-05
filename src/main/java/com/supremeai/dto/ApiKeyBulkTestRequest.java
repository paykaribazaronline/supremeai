package com.supremeai.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ApiKeyBulkTestRequest {
    @NotNull(message = "Key IDs list is required")
    @NotEmpty(message = "Key IDs list cannot be empty")
    @Size(min = 1, max = 100, message = "Key IDs list must contain between 1 and 100 items")
    private List<String> keyIds;

    public List<String> getKeyIds() {
        return keyIds;
    }

    public void setKeyIds(List<String> keyIds) {
        this.keyIds = keyIds;
    }
}