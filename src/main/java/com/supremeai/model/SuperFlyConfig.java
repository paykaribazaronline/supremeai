package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.ArrayList;
import java.util.List;

@Document(collectionName = "system_configs")
public class SuperFlyConfig {

  @DocumentId private String id = "superfly_settings";

  private String downloadUrl =
      "https://github.com/your-github-user/supremeai/releases/download/v2.0/superfly-94m.onnx";
  private String fileSize = "188 MB";
  private String version = "v1.0.0";
  private List<String> benefits = new ArrayList<>();
  private String guideline =
      "### SuperFly এজ এআই নির্দেশিকা\n\n"
          + "SuperFly হলো একটি অত্যন্ত লাইটওয়েট এবং অন-ডিভাইস ৯৪ মিলিয়ন প্যারামিটার মডেল।\n\n"
          + "#### ব্যবহারের সুবিধা:\n"
          + "১. **সম্পূর্ণ অফলাইন:** কোনো ইন্টারনেট বা ক্লাউড API কী ছাড়া কাজ করে।\n"
          + "২. **ল্যাটেন্সি ০.১ সেকেন্ড:** চোখের পলকে উত্তর দেয়।\n"
          + "৩. **সম্পূর্ণ নিরাপদ:** আপনার ডেটা আপনার ডিভাইসের বাইরে কোথাও শেয়ার হয় না।\n\n"
          + "#### লোকাল বাইনারি রান করার কমান্ড:\n"
          + "```bash\n"
          + "./superfly-sidecar --model ./superfly-94m.onnx --port 8082\n"
          + "```";

  public SuperFlyConfig() {
    benefits.add("চোখের পলকে রেসপন্স (ল্যাটেন্সি মাত্র ০.১ সেকেন্ড)");
    benefits.add("শতভাগ কস্ট সেভিং (কোনো ক্লাউড এপিআই বিল নেই)");
    benefits.add("নিরাপদ ও প্রাইভেট (ডেটা লোকাল ডিভাইসেই প্রসেস হয়)");
    benefits.add("অফলাইন মোড (ইন্টারনেট কানেকশন ছাড়াও চ্যাট সচল থাকে)");
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  public String getFileSize() {
    return fileSize;
  }

  public void setFileSize(String fileSize) {
    this.fileSize = fileSize;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<String> getBenefits() {
    return benefits;
  }

  public void setBenefits(List<String> benefits) {
    this.benefits = benefits;
  }

  public String getGuideline() {
    return guideline;
  }

  public void setGuideline(String guideline) {
    this.guideline = guideline;
  }
}
