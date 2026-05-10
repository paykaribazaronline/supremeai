package com.supremeai.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Handles frontend SPA routing. Forwards all non-asset frontend routes
 * to the appropriate index.html so React Router can take over.
 *
 * Pattern [^\\.]*  matches path segments that don't contain a '.'
 * (i.e., not a static file request like .js or .css).
 */
@Controller
public class ViewController {

    @GetMapping({"/", "/login"})
    public String login() {
        return "forward:/login.html";
    }

    /**
     * Covers /admin, /admin/, and up to 4 levels of sub-paths.
     * Deep nesting ensures React Router handles all client-side navigation.
     */
    @GetMapping({
        "/admin",
        "/admin/",
        "/admin/{p1:[^\\.]*}",
        "/admin/{p1:[^\\.]*}/{p2:[^\\.]*}",
        "/admin/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}",
        "/admin/{p1:[^\\.]*}/{p2:[^\\.]*}/{p3:[^\\.]*}/{p4:[^\\.]*}"
    })
    public String admin() {
        return "forward:/admin/index.html";
    }

    @GetMapping("/customer")
    public String customer() {
        return "forward:/customer.html";
    }
}
