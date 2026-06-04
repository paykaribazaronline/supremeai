package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

/**
 * Metadata for files stored in external storage (Telegram/Supabase). This is stored in Firestore to
 * allow the dashboard to list and manage these files. Collection: "storage_metadata"
 */
@Document(collectionName = "storage_metadata")
public class StorageMetadata {

  @DocumentId private String id;
  private String fileName;
  private String remotePath;
  private String storageProvider; // "TELEGRAM" or "SUPABASE"
  private String category; // "CODEBASE", "CHAT", "LEARNING", "ARTIFACT"
  private String userId; // Optional, for user-specific archives
  private long size;
  private String downloadUrl;
  private String contentType;
  private java.util.Date createdAt;
  private String timestamp; // ISO string for UI convenience

  public StorageMetadata() {
    this.createdAt = new java.util.Date();
    this.timestamp = java.time.Instant.now().toString();
  }

  public StorageMetadata(
      String fileName, String remotePath, String storageProvider, String category, long size) {
    this();
    this.fileName = fileName;
    this.remotePath = remotePath;
    this.storageProvider = storageProvider;
    this.category = category;
    this.size = size;
  }

  // Getters and Setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getRemotePath() {
    return remotePath;
  }

  public void setRemotePath(String remotePath) {
    this.remotePath = remotePath;
  }

  public String getStorageProvider() {
    return storageProvider;
  }

  public void setStorageProvider(String storageProvider) {
    this.storageProvider = storageProvider;
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public long getSize() {
    return size;
  }

  public void setSize(long size) {
    this.size = size;
  }

  public String getDownloadUrl() {
    return downloadUrl;
  }

  public void setDownloadUrl(String downloadUrl) {
    this.downloadUrl = downloadUrl;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public java.util.Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(java.util.Date createdAt) {
    this.createdAt = createdAt;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }
}
