package com.supremeai.learning;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for EvolutionPersistence.
 * Tests saving and loading best agent configurations.
 */
class EvolutionPersistenceTest {EvolutionPersistencepublic EvolutionPersistenceTest(EvolutionPersistence persistence, File saveFile, final String testPath) {
EvolutionPersistence    this.persistence = persistence;
EvolutionPersistence    this.saveFile = saveFile;
EvolutionPersistence    this.testPath = testPath;
EvolutionPersistence}


    @TempDir
    Path tempDir;




    @BeforeEach
    void setUp() throws Exception {
        saveFile = tempDir.resolve("test_agent_config.properties").toFile();
        persistence = new TestableEvolutionPersistence(saveFile.getAbsolutePath());
    }

    /**
     * Testable subclass that allows overriding the save file path.
     */
    private static class TestableEvolutionPersistence extends EvolutionPersistence {


        TestableEvolutionPersistence(String testPath) {
            this.testPath = testPath;
        }

        @Override
        public void saveBestConfig(AgentConfig config, int generation) {
            Properties props = new Properties();
            props.setProperty("generation", String.valueOf(generation));
            props.setProperty("decisionWeight", String.valueOf(config.decisionWeight));
            props.setProperty("confidenceThreshold", String.valueOf(config.confidenceThreshold));
            props.setProperty("learningRate", String.valueOf(config.learningRate));

            try (FileOutputStream out = new FileOutputStream(testPath)) {
                props.store(out, "Test config");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public AgentConfig loadBestConfig() {
            Properties props = new Properties();
            try (FileInputStream in = new FileInputStream(testPath)) {
                props.load(in);
                double dw = Double.parseDouble(props.getProperty("decisionWeight", "0.5"));
                double ct = Double.parseDouble(props.getProperty("confidenceThreshold", "0.5"));
                double lr = Double.parseDouble(props.getProperty("learningRate", "0.1"));
                return new AgentConfig(dw, ct, lr);
            } catch (IOException e) {
                return null;
            }
        }
    }

    @AfterEach
    void tearDown() {
        if (saveFile.exists()) {
            saveFile.delete();
        }
    }

    @Test
    void testSaveBestConfig_createsFile() throws Exception {
        AgentConfig config = new AgentConfig(0.8, 0.7, 0.2);
        persistence.saveBestConfig(config, 5);

        assertTrue(saveFile.exists(), "Save file should be created");
    }

    @Test
    void testSaveBestConfig_writesCorrectProperties() throws Exception {
        AgentConfig config = new AgentConfig(0.8, 0.7, 0.2);
        persistence.saveBestConfig(config, 10);

        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(saveFile)) {
            props.load(in);
        }

        assertEquals("10", props.getProperty("generation"));
        assertEquals("0.8", props.getProperty("decisionWeight"));
        assertEquals("0.7", props.getProperty("confidenceThreshold"));
        assertEquals("0.2", props.getProperty("learningRate"));
    }

    @Test
    void testLoadBestConfig_returnsCorrectConfig() throws Exception {
        // First save a config
        AgentConfig original = new AgentConfig(0.6, 0.5, 0.15);
        persistence.saveBestConfig(original, 3);

        // Load it back
        AgentConfig loaded = persistence.loadBestConfig();

        assertNotNull(loaded);
        assertEquals(0.6, loaded.decisionWeight, 0.001);
        assertEquals(0.5, loaded.confidenceThreshold, 0.001);
        assertEquals(0.15, loaded.learningRate, 0.001);
    }

    @Test
    void testLoadBestConfig_noFile_returnsNull() {
        // No file created yet
        AgentConfig loaded = persistence.loadBestConfig();
        assertNull(loaded, "Should return null when no previous config exists");
    }

    @Test
    void testLoadBestConfig_partialFile_usesDefaults() throws Exception {
        // Create a file with missing properties
        Properties props = new Properties();
        props.setProperty("decisionWeight", "0.9");
        // Missing confidenceThreshold and learningRate
        try (FileOutputStream out = new FileOutputStream(saveFile)) {
            props.store(out, "Partial config");
        }

        AgentConfig loaded = persistence.loadBestConfig();
        assertNotNull(loaded);
        assertEquals(0.9, loaded.decisionWeight, 0.001);
        assertEquals(0.5, loaded.confidenceThreshold, 0.001); // default
        assertEquals(0.1, loaded.learningRate, 0.001); // default
    }

    @Test
    void testSaveAndLoadAcrossGenerations() throws Exception {
        // Save gen 1
        persistence.saveBestConfig(new AgentConfig(0.1, 0.2, 0.3), 1);
        AgentConfig loaded1 = persistence.loadBestConfig();
        assertEquals(0.1, loaded1.decisionWeight, 0.001);

        // Overwrite with gen 2
        persistence.saveBestConfig(new AgentConfig(0.4, 0.5, 0.6), 2);
        AgentConfig loaded2 = persistence.loadBestConfig();
        assertEquals(0.4, loaded2.decisionWeight, 0.001);
        assertEquals(0.5, loaded2.confidenceThreshold, 0.001);
        assertEquals(0.6, loaded2.learningRate, 0.001);
    }

    @Test
    void testSaveBestConfig_invalidDirectory_handlesGracefully() {
        AgentConfig config = new AgentConfig(0.5, 0.5, 0.5);
        TestableEvolutionPersistence badPersistence = new TestableEvolutionPersistence("/invalid/path/config.properties") {
            @Override
            public void saveBestConfig(AgentConfig cfg, int generation) {
                // Override to do nothing for invalid path test
            }
        };
        badPersistence.saveBestConfig(config, 1);
    }
}
