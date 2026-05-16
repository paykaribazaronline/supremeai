package com.supremeai.service.analysis;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class GitDiffServiceTest {

    private final GitDiffService gitDiffService = new GitDiffService();

    @Test
    void testParseDiffOutputEmpty() {
        List<GitDiffService.FileDiff> diffs = gitDiffService.parseDiffOutput("");
        assertTrue(diffs.isEmpty());
    }

    @Test
    void testParseDiffOutputNull() {
        List<GitDiffService.FileDiff> diffs = gitDiffService.parseDiffOutput(null);
        assertTrue(diffs.isEmpty());
    }

    @Test
    void testParseDiffOutputModify() {
        String diffOutput = "diff --git a/src/Main.java b/src/Main.java\n"
            + "index abc1234..def5678 100644\n"
            + "--- a/src/Main.java\n"
            + "+++ b/src/Main.java\n"
            + "@@ -10,7 +10,7 @@\n"
            + " public class Main {\n"
            + "-    int x = 1;\n"
            + "+    int x = 2;\n"
            + " }\n";

        List<GitDiffService.FileDiff> diffs = gitDiffService.parseDiffOutput(diffOutput);
        assertFalse(diffs.isEmpty());
        assertEquals("src/Main.java", diffs.get(0).getFileName());
        assertEquals(GitDiffService.ChangeType.MODIFY, diffs.get(0).getChangeType());
    }

    @Test
    void testParseDiffOutputAdd() {
        String diffOutput = "diff --git a/dev/null b/src/New.java\n"
            + "new file mode 100644\n"
            + "--- /dev/null\n"
            + "+++ b/src/New.java\n"
            + "@@ -0,0 +1,3 @@\n"
            + "+public class New {\n"
            + "+}\n";

        List<GitDiffService.FileDiff> diffs = gitDiffService.parseDiffOutput(diffOutput);
        assertFalse(diffs.isEmpty());
        assertEquals("src/New.java", diffs.get(0).getFileName());
        assertEquals(GitDiffService.ChangeType.ADD, diffs.get(0).getChangeType());
    }

    @Test
    void testParseDiffOutputDelete() {
        String diffOutput = "diff --git a/src/Old.java b/dev/null\n"
            + "deleted file mode 100644\n"
            + "--- a/src/Old.java\n"
            + "+++ /dev/null\n"
            + "@@ -1,3 +0,0 @@\n"
            + "-public class Old {\n"
            + "-}\n";

        List<GitDiffService.FileDiff> diffs = gitDiffService.parseDiffOutput(diffOutput);
        assertFalse(diffs.isEmpty());
        assertEquals("src/Old.java", diffs.get(0).getFileName());
        assertEquals(GitDiffService.ChangeType.DELETE, diffs.get(0).getChangeType());
    }

    @Test
    void testParseDiffOutputMultipleFiles() {
        String diffOutput = "diff --git a/src/A.java b/src/A.java\n"
            + "--- a/src/A.java\n"
            + "+++ b/src/A.java\n"
            + "@@ -1,3 +1,3 @@\n"
            + "-old line\n"
            + "+new line\n"
            + "diff --git a/src/B.java b/src/B.java\n"
            + "--- a/src/B.java\n"
            + "+++ b/src/B.java\n"
            + "@@ -1,3 +1,3 @@\n"
            + "-old b\n"
            + "+new b\n";

        List<GitDiffService.FileDiff> diffs = gitDiffService.parseDiffOutput(diffOutput);
        assertEquals(2, diffs.size());
        assertEquals("src/A.java", diffs.get(0).getFileName());
        assertEquals("src/B.java", diffs.get(1).getFileName());
    }
}
