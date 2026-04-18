package com.supremeai.provider;

import java.util.Map;

public interface AIProvider {
    String getName();
    Map<String, Object> getCapabilities();
    String generate(String prompt);
}
