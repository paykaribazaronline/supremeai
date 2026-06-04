package com.supremeai.dto;

public class ApiKeyBulkRegenerateResult {
  private String keyId;
  private String label;
  private String status;
  private String newMaskedKey;

  public ApiKeyBulkRegenerateResult() {}

  public ApiKeyBulkRegenerateResult(
      String keyId, String label, String status, String newMaskedKey) {
    this.keyId = keyId;
    this.label = label;
    this.status = status;
    this.newMaskedKey = newMaskedKey;
  }

  public String getKeyId() {
    return keyId;
  }

  public void setKeyId(String keyId) {
    this.keyId = keyId;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getNewMaskedKey() {
    return newMaskedKey;
  }

  public void setNewMaskedKey(String newMaskedKey) {
    this.newMaskedKey = newMaskedKey;
  }
}
