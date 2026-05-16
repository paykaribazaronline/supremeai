package com.supremeai.service.analysis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

class CodeChunkerServiceTest {

    private final CodeChunkerService chunkerService = new CodeChunkerService();

    @TempDir
    File tempDir;

    @Test
    void testChunkFileBasic() throws IOException {
        File testFile = new File(tempDir, "Test.java");
        try (FileWriter writer = new FileWriter(testFile)) {
            for (int i = 0; i < 250; i++) {
                writer.write("line " + i + "\n");
            }
        }

        List<CodeChunkData> chunks = chunkerService.chunkFile(testFile, "Test.java", 100, 10);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 2);
        assertEquals(1, chunks.get(0).getStartLine());
        assertEquals("Test.java", chunks.get(0).getFile());
        assertEquals("java", chunks.get(0).getLanguage());
    }

    @Test
    void testChunkFileSmallFile() throws IOException {
        File testFile = new File(tempDir, "Small.py");
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("def hello():\n    pass\n");
        }

        List<CodeChunkData> chunks = chunkerService.chunkFile(testFile, "Small.py");
        assertEquals(1, chunks.size());
        assertEquals("python", chunks.get(0).getLanguage());
    }

    @Test
    void testChunkFileWithOverlap() throws IOException {
        File testFile = new File(tempDir, "Overlap.js");
        try (FileWriter writer = new FileWriter(testFile)) {
            for (int i = 0; i < 50; i++) {
                writer.write("const x" + i + " = " + i + ";\n");
            }
        }

        List<CodeChunkData> chunks = chunkerService.chunkFile(testFile, "Overlap.js", 20, 5);
        assertTrue(chunks.size() > 1);

        for (int i = 1; i < chunks.size(); i++) {
            assertTrue(chunks.get(i).getStartLine() <= chunks.get(i - 1).getEndLine());
        }
    }

    @Test
    void testSemanticChunking() throws IOException {
        File testFile = new File(tempDir, "Semantic.java");
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write("public class Foo {\n");
            writer.write("  public void method1() {\n");
            writer.write("    // body\n");
            writer.write("  }\n");
            writer.write("  public void method2() {\n");
            writer.write("    // body\n");
            writer.write("  }\n");
            writer.write("}\n");
        }

        List<CodeChunkData> chunks = chunkerService.chunkFileSemantic(testFile, "Semantic.java");
        assertFalse(chunks.isEmpty());
    }

    @Test
    void testLanguageDetection() throws IOException {
        File javaFile = new File(tempDir, "test.JAVA");
        try (FileWriter writer = new FileWriter(javaFile)) {
            writer.write("class Test {}");
        }
        List<CodeChunkData> chunks = chunkerService.chunkFile(javaFile, "test.JAVA");
        assertEquals("java", chunks.get(0).getLanguage());
    }
}
