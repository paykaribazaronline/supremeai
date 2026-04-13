package org.example.controller;

import org.example.service.SuggestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/suggestion")
public class suggestionController {

    @Autowired
    private SuggestionService suggestionService;

}
