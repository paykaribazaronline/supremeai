package com.supremeai.config;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

/**
 * Blue-green deployment configuration. Supports zero-downtime deployments with traffic switching.
 */
@Configuration
public class BlueGreenDeploymentConfig implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(BlueGreenDeploymentConfig.class);

  @Value("${deployment.strategy:blue-green}")
  private String deploymentStrategy;

  @Value("${deployment.color:blue}")
  private String deploymentColor;

  @Value("${deployment.version:1.0.0}")
  private String deploymentVersion;

  // Track active deployment
  private final AtomicReference<DeploymentInfo> activeDeployment = new AtomicReference<>();

  @Override
  public void run(ApplicationArguments args) {
    DeploymentInfo info =
        DeploymentInfo.builder()
            .color(deploymentColor)
            .version(deploymentVersion)
            .strategy(deploymentStrategy)
            .startTime(Instant.now())
            .build();

    activeDeployment.set(info);
    log.info(
        "Active deployment: {} (version: {}, strategy: {})",
        deploymentColor,
        deploymentVersion,
        deploymentStrategy);
  }

  /** Get current active deployment info. */
  public DeploymentInfo getActiveDeployment() {
    return activeDeployment.get();
  }

  /** Switch traffic to new deployment (blue-green switch). */
  public void switchTraffic(String newColor, String newVersion) {
    DeploymentInfo current = activeDeployment.get();
    DeploymentInfo newDeployment =
        DeploymentInfo.builder()
            .color(newColor)
            .version(newVersion)
            .strategy(deploymentStrategy)
            .startTime(Instant.now())
            .previousColor(current.getColor())
            .previousVersion(current.getVersion())
            .build();

    activeDeployment.set(newDeployment);
    log.info("Switched traffic to {} (version: {})", newColor, newVersion);
  }

  /** Rollback to previous deployment. */
  public void rollback() {
    DeploymentInfo current = activeDeployment.get();
    if (current.getPreviousColor() != null) {
      DeploymentInfo rollbackDeployment =
          DeploymentInfo.builder()
              .color(current.getPreviousColor())
              .version(current.getPreviousVersion())
              .strategy(deploymentStrategy)
              .startTime(Instant.now())
              .build();

      activeDeployment.set(rollbackDeployment);
      log.warn(
          "Rolled back to {} (version: {})",
          current.getPreviousColor(),
          current.getPreviousVersion());
    }
  }

  public static class DeploymentInfo {
    private String color;
    private String version;
    private String strategy;
    private Instant startTime;
    private String previousColor;
    private String previousVersion;

    public DeploymentInfo() {}

    public DeploymentInfo(
        String color,
        String version,
        String strategy,
        Instant startTime,
        String previousColor,
        String previousVersion) {
      this.color = color;
      this.version = version;
      this.strategy = strategy;
      this.startTime = startTime;
      this.previousColor = previousColor;
      this.previousVersion = previousVersion;
    }

    public String getColor() {
      return color;
    }

    public void setColor(String color) {
      this.color = color;
    }

    public String getVersion() {
      return version;
    }

    public void setVersion(String version) {
      this.version = version;
    }

    public String getStrategy() {
      return strategy;
    }

    public void setStrategy(String strategy) {
      this.strategy = strategy;
    }

    public Instant getStartTime() {
      return startTime;
    }

    public void setStartTime(Instant startTime) {
      this.startTime = startTime;
    }

    public String getPreviousColor() {
      return previousColor;
    }

    public void setPreviousColor(String previousColor) {
      this.previousColor = previousColor;
    }

    public String getPreviousVersion() {
      return previousVersion;
    }

    public void setPreviousVersion(String previousVersion) {
      this.previousVersion = previousVersion;
    }

    public static DeploymentInfoBuilder builder() {
      return new DeploymentInfoBuilder();
    }

    public static class DeploymentInfoBuilder {
      private String color;
      private String version;
      private String strategy;
      private Instant startTime;
      private String previousColor;
      private String previousVersion;

      DeploymentInfoBuilder() {}

      public DeploymentInfoBuilder color(String color) {
        this.color = color;
        return this;
      }

      public DeploymentInfoBuilder version(String version) {
        this.version = version;
        return this;
      }

      public DeploymentInfoBuilder strategy(String strategy) {
        this.strategy = strategy;
        return this;
      }

      public DeploymentInfoBuilder startTime(Instant startTime) {
        this.startTime = startTime;
        return this;
      }

      public DeploymentInfoBuilder previousColor(String previousColor) {
        this.previousColor = previousColor;
        return this;
      }

      public DeploymentInfoBuilder previousVersion(String previousVersion) {
        this.previousVersion = previousVersion;
        return this;
      }

      public DeploymentInfo build() {
        return new DeploymentInfo(
            color, version, strategy, startTime, previousColor, previousVersion);
      }
    }
  }
}
