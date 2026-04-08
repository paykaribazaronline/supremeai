package org.example.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/improveService")
public class ImproveController {
    private static final Logger logger = LoggerFactory.getLogger(ImproveController.class);

    @Autowired
    private ImproveService improveService;

}
