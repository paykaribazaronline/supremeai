package com.supremeai.intelligence.healing;

import java.util.Objects;

public class BuildResult {
  private boolean success;
  private String errorLogs;
  private String failedStage; // "COMPILATION", "UNIT_TESTS", "LINTING"

  public BuildResult(boolean success, String errorLogs, String failedStage) {
    this.success = success;
    this.errorLogs = errorLogs;
    this.failedStage = failedStage;
  }

  public boolean isSuccess() {
    return success;
  }

  public String getErrorLogs() {
    return errorLogs;
  }

  public String getFailedStage() {
    return failedStage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    BuildResult that = (BuildResult) o;
    return success == that.success
        && Objects.equals(errorLogs, that.errorLogs)
        && Objects.equals(failedStage, that.failedStage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(success, errorLogs, failedStage);
  }

  @Override
  public String toString() {
    return "BuildResult{"
        + "success="
        + success
        + ", errorLogs='"
        + errorLogs
        + '\''
        + ", failedStage='"
        + failedStage
        + '\''
        + '}';
  }
}
