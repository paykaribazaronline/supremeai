package com.supremeai.model.analysis;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;

import java.util.List;

@Document(collectionName = "code_embeddings")
public class CodeChunk {
    @DocumentId
    private String id;
    private String projectId;
    private String file;
    private int startLine;
    private int endLine;
    private String content;
    private String hash;
    private String language;
    private List<Double> embedding;
    private String createdAt;

    public CodeChunk() {}

    public CodeChunk(String id, String projectId, String file, int startLine, int endLine, String content, String hash, String language, List<Double> embedding, String createdAt) {
        this.id = id;
        this.projectId = projectId;
        this.file = file;
        this.startLine = startLine;
        this.endLine = endLine;
        this.content = content;
        this.hash = hash;
        this.language = language;
        this.embedding = embedding;
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }
    public int getStartLine() { return startLine; }
    public void setStartLine(int startLine) { this.startLine = startLine; }
    public int getEndLine() { return endLine; }
    public void setEndLine(int endLine) { this.endLine = endLine; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public List<Double> getEmbedding() { return embedding; }
    public void setEmbedding(List<Double> embedding) { this.embedding = embedding; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String projectId;
        private String file;
        private int startLine;
        private int endLine;
        private String content;
        private String hash;
        private String language;
        private List<Double> embedding;
        private String createdAt;

        public Builder id(String id) { this.id = id; return this; }
        public Builder projectId(String projectId) { this.projectId = projectId; return this; }
        public Builder file(String file) { this.file = file; return this; }
        public Builder startLine(int startLine) { this.startLine = startLine; return this; }
        public Builder endLine(int endLine) { this.endLine = endLine; return this; }
        public Builder content(String content) { this.content = content; return this; }
        public Builder hash(String hash) { this.hash = hash; return this; }
        public Builder language(String language) { this.language = language; return this; }
        public Builder embedding(List<Double> embedding) { this.embedding = embedding; return this; }
        public Builder createdAt(String createdAt) { this.createdAt = createdAt; return this; }

        public CodeChunk build() {
            return new CodeChunk(id, projectId, file, startLine, endLine, content, hash, language, embedding, createdAt);
        }
    }
}
