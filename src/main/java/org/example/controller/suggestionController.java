package org.example.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/suggestionService")
public class suggestionController {
    private static final Logger logger = LoggerFactory.getLogger(suggestionController.class);

    @Autowired
    private suggestionService suggestionService;

}
