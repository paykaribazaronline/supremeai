package com.supremeai.intelligence.healing;

public class BuildResult {
    private boolean success;
    private String errorLogs;
    private String failedStage; // "COMPILATION", "UNIT_TESTS", "LINTING"

    public BuildResult(boolean success, String errorLogs, String failedStage) {
        this.success = success;
        this.errorLogs = errorLogs;
        this.failedStage = failedStage;
    }

    public boolean isSuccess() { return success; }
    public String getErrorLogs() { return errorLogs; }
    public String getFailedStage() { return failedStage; }
}