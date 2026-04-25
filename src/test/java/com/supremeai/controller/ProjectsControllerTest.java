
package com.supremeai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.dto.ProjectDTO;
import com.supremeai.service.ProjectService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProjectsController.class)
public class ProjectsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProjectService projectService;

    @Autowired
    private ObjectMapper objectMapper;

    private ProjectDTO projectDTO;

    @BeforeEach
    public void setUp() {
        projectDTO = new ProjectDTO();
        projectDTO.setId("project1");
        projectDTO.setName("Test Project");
        projectDTO.setDescription("A test project");
        projectDTO.setUserId("user1");
        projectDTO.setCreatedAt(LocalDateTime.now());
        projectDTO.setUpdatedAt(LocalDateTime.now());
        projectDTO.setStatus("ACTIVE");
    }

    @Test
    @WithMockUser(username = "user1")
    public void testCreateProject_Success() throws Exception {
        // Arrange
        when(projectService.createProject(any(ProjectDTO.class), eq("user1")))
                .thenReturn(projectDTO);

        // Act & Assert
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("project1"))
                .andExpect(jsonPath("$.name").value("Test Project"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @WithMockUser(username = "user1")
    public void testCreateProject_InvalidData() throws Exception {
        // Arrange
        projectDTO.setName(""); // Empty name

        // Act & Assert
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectDTO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user1")
    public void testGetAllProjects_Success() throws Exception {
        // Arrange
        List<ProjectDTO> projects = Arrays.asList(projectDTO);
        when(projectService.getAllProjects(eq("user1"))).thenReturn(projects);

        // Act & Assert
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("project1"))
                .andExpect(jsonPath("$[0].name").value("Test Project"));
    }

    @Test
    @WithMockUser(username = "user1")
    public void testGetProjectById_Success() throws Exception {
        // Arrange
        when(projectService.getProjectById(eq("project1"), eq("user1")))
                .thenReturn(projectDTO);

        // Act & Assert
        mockMvc.perform(get("/api/projects/project1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("project1"))
                .andExpect(jsonPath("$.name").value("Test Project"));
    }

    @Test
    @WithMockUser(username = "user1")
    public void testGetProjectById_NotFound() throws Exception {
        // Arrange
        when(projectService.getProjectById(eq("nonexistent"), eq("user1")))
                .thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/projects/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1")
    public void testUpdateProject_Success() throws Exception {
        // Arrange
        projectDTO.setName("Updated Project");
        when(projectService.updateProject(eq("project1"), any(ProjectDTO.class), eq("user1")))
                .thenReturn(projectDTO);

        // Act & Assert
        mockMvc.perform(put("/api/projects/project1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(projectDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Project"));
    }

    @Test
    @WithMockUser(username = "user1")
    public void testDeleteProject_Success() throws Exception {
        // Arrange
        when(projectService.deleteProject(eq("project1"), eq("user1")))
                .thenReturn(true);

        // Act & Assert
        mockMvc.perform(delete("/api/projects/project1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user1")
    public void testDeleteProject_NotFound() throws Exception {
        // Arrange
        when(projectService.deleteProject(eq("nonexistent"), eq("user1")))
                .thenReturn(false);

        // Act & Assert
        mockMvc.perform(delete("/api/projects/nonexistent"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user1")
    public void testGetProjectStats_Success() throws Exception {
        // Arrange
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalFiles", 15);
        stats.put("totalLines", 2500);
        stats.put("lastModified", LocalDateTime.now().toString());

        when(projectService.getProjectStats(eq("project1"), eq("user1")))
                .thenReturn(stats);

        // Act & Assert
        mockMvc.perform(get("/api/projects/project1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalFiles").value(15))
                .andExpect(jsonPath("$.totalLines").value(2500));
    }

    @Test
    @WithMockUser(username = "user1")
    public void testSearchProjects_Success() throws Exception {
        // Arrange
        List<ProjectDTO> projects = Arrays.asList(projectDTO);
        when(projectService.searchProjects(eq("test"), eq("user1")))
                .thenReturn(projects);

        // Act & Assert
        mockMvc.perform(get("/api/projects/search?query=test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Test Project"));
    }

    @Test
    @WithMockUser(username = "user1")
    public void testSearchProjects_EmptyResults() throws Exception {
        // Arrange
        when(projectService.searchProjects(eq("nonexistent"), eq("user1")))
                .thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/api/projects/search?query=nonexistent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }
}
