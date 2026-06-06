package com.supremeai.simulator;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a device configuration profile for simulator sessions. Includes hardware specs, OS
 * version, screen resolution, and network conditions.
 */
public class DeviceProfile implements Serializable {
  private static final long serialVersionUID = 1L;

  private String profileId;
  private String deviceName;
  private String deviceType;
  private String os;
  private String osVersion;
  private String screenResolution;
  private Integer densityDpi;
  private Double cpuCores;
  private Double memoryMb;
  private String networkType;
  private Integer networkSpeedMbps;
  private Map<String, Object> capabilities = new HashMap<>();

  public DeviceProfile() {}

  public DeviceProfile(
      String profileId,
      String deviceName,
      String deviceType,
      String os,
      String osVersion,
      String screenResolution,
      Integer densityDpi) {
    this.profileId = profileId;
    this.deviceName = deviceName;
    this.deviceType = deviceType;
    this.os = os;
    this.osVersion = osVersion;
    this.screenResolution = screenResolution;
    this.densityDpi = densityDpi;
  }

  public String getProfileId() {
    return profileId;
  }

  public DeviceProfile setProfileId(String profileId) {
    this.profileId = profileId;
    return this;
  }

  public String getDeviceName() {
    return deviceName;
  }

  public DeviceProfile setDeviceName(String deviceName) {
    this.deviceName = deviceName;
    return this;
  }

  public String getDeviceType() {
    return deviceType;
  }

  public DeviceProfile setDeviceType(String deviceType) {
    this.deviceType = deviceType;
    return this;
  }

  public String getOs() {
    return os;
  }

  public DeviceProfile setOs(String os) {
    this.os = os;
    return this;
  }

  public String getOsVersion() {
    return osVersion;
  }

  public DeviceProfile setOsVersion(String osVersion) {
    this.osVersion = osVersion;
    return this;
  }

  public String getScreenResolution() {
    return screenResolution;
  }

  public DeviceProfile setScreenResolution(String screenResolution) {
    this.screenResolution = screenResolution;
    return this;
  }

  public Integer getDensityDpi() {
    return densityDpi;
  }

  public DeviceProfile setDensityDpi(Integer densityDpi) {
    this.densityDpi = densityDpi;
    return this;
  }

  public Double getCpuCores() {
    return cpuCores;
  }

  public DeviceProfile setCpuCores(Double cpuCores) {
    this.cpuCores = cpuCores;
    return this;
  }

  public Double getMemoryMb() {
    return memoryMb;
  }

  public DeviceProfile setMemoryMb(Double memoryMb) {
    this.memoryMb = memoryMb;
    return this;
  }

  public String getNetworkType() {
    return networkType;
  }

  public DeviceProfile setNetworkType(String networkType) {
    this.networkType = networkType;
    return this;
  }

  public Integer getNetworkSpeedMbps() {
    return networkSpeedMbps;
  }

  public DeviceProfile setNetworkSpeedMbps(Integer networkSpeedMbps) {
    this.networkSpeedMbps = networkSpeedMbps;
    return this;
  }

  public Map<String, Object> getCapabilities() {
    return capabilities;
  }

  public DeviceProfile setCapabilities(Map<String, Object> capabilities) {
    this.capabilities = capabilities;
    return this;
  }

  public DeviceProfile addCapability(String key, Object value) {
    this.capabilities.put(key, value);
    return this;
  }
}
