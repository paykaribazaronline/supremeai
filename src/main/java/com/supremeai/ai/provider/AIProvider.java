package com.supremeai.ai.provider;

import com.supremeai.dto.AISolution;
import com.supremeai.dto.ProblemStatement;

public interface AIProvider {
    String getId();
    AISolution solve(ProblemStatement problem);
}
