package com.supremeai.service.analysis;

public class CodeChunkData {
  private String id;
  private String file;
  private int startLine;
  private int endLine;
  private String content;
  private String hash;
  private String language;

  public CodeChunkData() {}

  public CodeChunkData(
      String id,
      String file,
      int startLine,
      int endLine,
      String content,
      String hash,
      String language) {
    this.id = id;
    this.file = file;
    this.startLine = startLine;
    this.endLine = endLine;
    this.content = content;
    this.hash = hash;
    this.language = language;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public int getStartLine() {
    return startLine;
  }

  public void setStartLine(int startLine) {
    this.startLine = startLine;
  }

  public int getEndLine() {
    return endLine;
  }

  public void setEndLine(int endLine) {
    this.endLine = endLine;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getHash() {
    return hash;
  }

  public void setHash(String hash) {
    this.hash = hash;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private String id;
    private String file;
    private int startLine;
    private int endLine;
    private String content;
    private String hash;
    private String language;

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder file(String file) {
      this.file = file;
      return this;
    }

    public Builder startLine(int startLine) {
      this.startLine = startLine;
      return this;
    }

    public Builder endLine(int endLine) {
      this.endLine = endLine;
      return this;
    }

    public Builder content(String content) {
      this.content = content;
      return this;
    }

    public Builder hash(String hash) {
      this.hash = hash;
      return this;
    }

    public Builder language(String language) {
      this.language = language;
      return this;
    }

    public CodeChunkData build() {
      return new CodeChunkData(id, file, startLine, endLine, content, hash, language);
    }
  }
}
