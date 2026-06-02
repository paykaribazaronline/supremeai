package com.supremeai.controller;

import com.supremeai.model.UserGuide;
import com.supremeai.repository.UserGuideRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuideControllerTest {UserGuideRepositorypublic GuideControllerTest(UserGuideRepository userGuideRepository, GuideController controller) {
UserGuideRepository    this.userGuideRepository = userGuideRepository;
UserGuideRepository    this.controller = controller;
UserGuideRepository}






    @BeforeEach
    void setUp() throws Exception {
        controller = new GuideController();
        java.lang.reflect.Field field = GuideController.class.getDeclaredField("userGuideRepository");
        field.setAccessible(true);
        field.set(controller, userGuideRepository);
    }

    @Test
    void getAllGuides_shouldReturnPublishedGuides() {
        UserGuide g1 = new UserGuide("g1", Map.of("en", "Guide 1"), Map.of("en", "Desc 1"));
        g1.setIsPublished(true);
        UserGuide g2 = new UserGuide("g2", Map.of("en", "Guide 2"), Map.of("en", "Desc 2"));
        g2.setIsPublished(true);

        when(userGuideRepository.findByIsPublished(true)).thenReturn(Flux.just(g1, g2));

        StepVerifier.create(controller.getAllGuides())
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void getAllGuides_shouldReturnEmpty_whenNonePublished() {
        when(userGuideRepository.findByIsPublished(true)).thenReturn(Flux.empty());

        StepVerifier.create(controller.getAllGuides())
                .verifyComplete();
    }

    @Test
    void getGuide_shouldReturnPublishedGuide() {
        UserGuide guide = new UserGuide("g1", Map.of("en", "Guide 1"), Map.of("en", "Desc 1"));
        guide.setIsPublished(true);

        when(userGuideRepository.findById("g1")).thenReturn(Mono.just(guide));

        StepVerifier.create(controller.getGuide("g1"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.OK, response.getStatusCode());
                    assertNotNull(response.getBody());
                    assertEquals("g1", response.getBody().getId());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getGuide_shouldReturnNotFound_whenNotPublished() {
        UserGuide guide = new UserGuide("g1", Map.of("en", "Guide 1"), Map.of("en", "Desc 1"));
        guide.setIsPublished(false);

        when(userGuideRepository.findById("g1")).thenReturn(Mono.just(guide));

        StepVerifier.create(controller.getGuide("g1"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getGuide_shouldReturnNotFound_whenNotExists() {
        when(userGuideRepository.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(controller.getGuide("missing"))
                .expectNextMatches(response -> {
                    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
                    return true;
                })
                .verifyComplete();
    }

    @Test
    void getGuidesByCategory_shouldReturnMatchingGuides() {
        UserGuide g = new UserGuide("g1", Map.of("en", "Basics"), Map.of("en", "Basic guide"));
        g.setCategory("basics");
        g.setIsPublished(true);

        when(userGuideRepository.findByCategoryAndIsPublished("basics", true)).thenReturn(Flux.just(g));

        StepVerifier.create(controller.getGuidesByCategory("basics"))
                .expectNextMatches(guide -> "basics".equals(guide.getCategory()))
                .verifyComplete();
    }

    @Test
    void getGuidesByCategory_shouldReturnEmpty_whenNoMatch() {
        when(userGuideRepository.findByCategoryAndIsPublished("nonexistent", true)).thenReturn(Flux.empty());

        StepVerifier.create(controller.getGuidesByCategory("nonexistent"))
                .verifyComplete();
    }

    @Test
    void getGuidesByTag_shouldReturnGuidesWithTag() {
        UserGuide g = new UserGuide("g1", Map.of("en", "API Guide"), Map.of("en", "API docs"));
        g.setTags(List.of("api", "rest"));

        when(userGuideRepository.findByTagsContaining("api")).thenReturn(Flux.just(g));

        StepVerifier.create(controller.getGuidesByTag("api"))
                .expectNextMatches(guide -> guide.getTags().contains("api"))
                .verifyComplete();
    }

    @Test
    void getGuidesByTag_shouldReturnEmpty_whenNoMatch() {
        when(userGuideRepository.findByTagsContaining("nonexistent")).thenReturn(Flux.empty());

        StepVerifier.create(controller.getGuidesByTag("nonexistent"))
                .verifyComplete();
    }

    @Test
    void getAvailableLanguages_shouldReturnEnglishAndBangla() {
        ResponseEntity<Map<String, String>> response = controller.getAvailableLanguages();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("English", response.getBody().get("en"));
        assertEquals("বাংলা (Bangla)", response.getBody().get("bn"));
        assertEquals(2, response.getBody().size());
    }
}
