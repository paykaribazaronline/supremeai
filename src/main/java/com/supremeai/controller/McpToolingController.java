package com.supremeai.controller;

import com.supremeai.model.SystemLearning;
import com.supremeai.service.FirecrawlService;
import com.supremeai.service.KnowledgeRetrievalService;
import com.supremeai.service.ProjectContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * McpToolingController exposes SupremeAI's internal tools and local knowledge
 * as an MCP (Model Context Protocol) server. This allows AI agents to
 * directly interact with browser scraping and local knowledge retrieval.
 */
@RestController
@RequestMapping("/api/mcp")
@RequiredArgsConstructor
public class McpToolingController {

    private final FirecrawlService firecrawlService;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final ProjectContextService projectContextService;

    /**
     * Exposes the project structure via MCP.
     */
    @GetMapping("/project/structure")
    public Mono<ResponseEntity<Map<String, String>>> getStructure() {
        return projectContextService.getProjectStructure()
                .map(s -> ResponseEntity.ok(Map.of("structure", s)));
    }

    /**
     * Reads a project file content via MCP.
     */
    @PostMapping("/project/read-file")
    public Mono<ResponseEntity<Map<String, String>>> readFile(@RequestBody Map<String, String> request) {
        return projectContextService.readFile(request.get("path"))
                .map(content -> ResponseEntity.ok(Map.of("content", content)))
                .onErrorResume(e -> Mono.just(ResponseEntity.badRequest().body(Map.of("error", e.getMessage()))));
    }

    /**
     * Scrapes a given URL and returns its content in LLM-ready markdown format.
     * This exposes the BROWSER_SCRAPPER functionality via MCP.
     *
     * @param request A map containing the "url" to scrape.
     * @return Mono of ResponseEntity containing the markdown content.
     */
    @PostMapping(value = "/scrape", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map<String, String>>> scrapeUrl(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        if (url == null || url.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of("error", "URL is required for scraping.")));
        }
        return firecrawlService.scrapeToMarkdown(url)
                .map(markdown -> ResponseEntity.ok(Map.of("markdown", markdown)))
                .onErrorResume(IllegalStateException.class, e ->
                        Mono.just(ResponseEntity.status(500).body(Map.of("error", e.getMessage()))))
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.internalServerError().body(Map.of("error", "Failed to scrape URL: " + e.getMessage()))));
    }

    /**
     * Searches the local knowledge base for entries matching the query.
     * This exposes the Local Knowledge functionality via MCP.
     *
     * @param request A map containing the "query" for knowledge search.
     * @return Flux of SystemLearning entries.
     */
    @PostMapping(value = "/knowledge/search", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<SystemLearning>>> searchKnowledge(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        if (query == null || query.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().body(List.of()));
        }
        return knowledgeRetrievalService.searchLocalKnowledge(query)
                .collectList()
                .map(ResponseEntity::ok)
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.internalServerError().body(List.of())));
    }

    /**
     * Retrieves a specific knowledge entry by its ID.
     * @param id The ID of the SystemLearning entry.
     * @return Mono of ResponseEntity containing the SystemLearning entry.
     */
    @GetMapping(value = "/knowledge/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<SystemLearning>> getKnowledgeById(@PathVariable String id) {
        return knowledgeRetrievalService.getKnowledgeById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e ->
                        Mono.just(ResponseEntity.internalServerError().build()));
    }
}