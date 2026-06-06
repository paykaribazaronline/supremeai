package com.supremeai.simulator;

import java.util.List;

/** Predefined device profiles for simulator sessions. */
public class DeviceProfiles {

  public static final DeviceProfile PIXEL_6 =
      new DeviceProfile()
          .setProfileId("PIXEL_6")
          .setDeviceName("Google Pixel 6")
          .setDeviceType("ANDROID")
          .setOs("Android")
          .setOsVersion("14")
          .setScreenResolution("1080x2400")
          .setDensityDpi(420)
          .setCpuCores(8.0)
          .setMemoryMb(8192.0)
          .setNetworkType("5G")
          .setNetworkSpeedMbps(200)
          .addCapability("biometric", true)
          .addCapability("nfc", true)
          .addCapability("bluetooth", true);

  public static final DeviceProfile PIXEL_7 =
      new DeviceProfile()
          .setProfileId("PIXEL_7")
          .setDeviceName("Google Pixel 7")
          .setDeviceType("ANDROID")
          .setOs("Android")
          .setOsVersion("14")
          .setScreenResolution("1080x2400")
          .setDensityDpi(420)
          .setCpuCores(8.0)
          .setMemoryMb(8192.0)
          .setNetworkType("5G")
          .setNetworkSpeedMbps(250)
          .addCapability("biometric", true)
          .addCapability("nfc", true);

  public static final DeviceProfile IPHONE_14 =
      new DeviceProfile()
          .setProfileId("IPHONE_14")
          .setDeviceName("iPhone 14")
          .setDeviceType("IOS")
          .setOs("iOS")
          .setOsVersion("17.0")
          .setScreenResolution("1170x2532")
          .setDensityDpi(460)
          .setCpuCores(6.0)
          .setMemoryMb(6144.0)
          .setNetworkType("5G")
          .setNetworkSpeedMbps(200)
          .addCapability("faceId", true)
          .addCapability("nfc", true)
          .addCapability("bluetooth", true);

  public static final DeviceProfile IPHONE_15_PRO =
      new DeviceProfile()
          .setProfileId("IPHONE_15_PRO")
          .setDeviceName("iPhone 15 Pro")
          .setDeviceType("IOS")
          .setOs("iOS")
          .setOsVersion("17.2")
          .setScreenResolution("1179x2556")
          .setDensityDpi(460)
          .setCpuCores(6.0)
          .setMemoryMb(8192.0)
          .setNetworkType("5G")
          .setNetworkSpeedMbps(300)
          .addCapability("faceId", true)
          .addCapability("nfc", true)
          .addCapability("usb3", true);

  public static final DeviceProfile IPAD_PRO =
      new DeviceProfile()
          .setProfileId("IPAD_PRO")
          .setDeviceName("iPad Pro 12.9")
          .setDeviceType("IOS")
          .setOs("iPadOS")
          .setOsVersion("17.0")
          .setScreenResolution("2048x2732")
          .setDensityDpi(264)
          .setCpuCores(8.0)
          .setMemoryMb(16384.0)
          .setNetworkType("WIFI")
          .setNetworkSpeedMbps(500)
          .addCapability("faceId", true)
          .addCapability("applePencil", true);

  public static final DeviceProfile DESKTOP_1080P =
      new DeviceProfile()
          .setProfileId("DESKTOP_1080P")
          .setDeviceName("Desktop 1080p")
          .setDeviceType("DESKTOP")
          .setOs("Windows")
          .setOsVersion("11")
          .setScreenResolution("1920x1080")
          .setDensityDpi(96)
          .setCpuCores(8.0)
          .setMemoryMb(16384.0)
          .setNetworkType("ETHERNET")
          .setNetworkSpeedMbps(1000)
          .addCapability("mouse", true)
          .addCapability("keyboard", true)
          .addCapability("touchscreen", false);

  public static final DeviceProfile DESKTOP_4K =
      new DeviceProfile()
          .setProfileId("DESKTOP_4K")
          .setDeviceName("Desktop 4K")
          .setDeviceType("DESKTOP")
          .setOs("macOS")
          .setOsVersion("14.0")
          .setScreenResolution("3840x2160")
          .setDensityDpi(192)
          .setCpuCores(12.0)
          .setMemoryMb(32768.0)
          .setNetworkType("WIFI")
          .setNetworkSpeedMbps(600)
          .addCapability("mouse", true)
          .addCapability("keyboard", true)
          .addCapability("retina", true);

  public static DeviceProfile resolve(String profileId) {
    return switch (profileId != null ? profileId.toUpperCase() : "PIXEL_6") {
      case "PIXEL_7" -> PIXEL_7;
      case "IPHONE_14" -> IPHONE_14;
      case "IPHONE_15_PRO" -> IPHONE_15_PRO;
      case "IPAD_PRO" -> IPAD_PRO;
      case "DESKTOP_4K" -> DESKTOP_4K;
      default -> PIXEL_6;
    };
  }

  public static List<DeviceProfile> getAllProfiles() {
    return List.of(PIXEL_6, PIXEL_7, IPHONE_14, IPHONE_15_PRO, IPAD_PRO, DESKTOP_1080P, DESKTOP_4K);
  }
}
