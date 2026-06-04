package com.supremeai.model.analysis;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.List;

@Document(collectionName = "dependency_graphs")
public class DependencyGraph {
  @DocumentId private String id;
  private String projectId;
  private String file;
  private List<String> imports;
  private List<String> importedBy;
  private String updatedAt;

  public DependencyGraph() {}

  public DependencyGraph(
      String id,
      String projectId,
      String file,
      List<String> imports,
      List<String> importedBy,
      String updatedAt) {
    this.id = id;
    this.projectId = projectId;
    this.file = file;
    this.imports = imports;
    this.importedBy = importedBy;
    this.updatedAt = updatedAt;
  }

  public static DependencyGraphBuilder builder() {
    return new DependencyGraphBuilder();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getProjectId() {
    return projectId;
  }

  public void setProjectId(String projectId) {
    this.projectId = projectId;
  }

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public List<String> getImports() {
    return imports;
  }

  public void setImports(List<String> imports) {
    this.imports = imports;
  }

  public List<String> getImportedBy() {
    return importedBy;
  }

  public void setImportedBy(List<String> importedBy) {
    this.importedBy = importedBy;
  }

  public String getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(String updatedAt) {
    this.updatedAt = updatedAt;
  }

  public static class DependencyGraphBuilder {
    private String id;
    private String projectId;
    private String file;
    private List<String> imports;
    private List<String> importedBy;
    private String updatedAt;

    public DependencyGraphBuilder id(String id) {
      this.id = id;
      return this;
    }

    public DependencyGraphBuilder projectId(String projectId) {
      this.projectId = projectId;
      return this;
    }

    public DependencyGraphBuilder file(String file) {
      this.file = file;
      return this;
    }

    public DependencyGraphBuilder imports(List<String> imports) {
      this.imports = imports;
      return this;
    }

    public DependencyGraphBuilder importedBy(List<String> importedBy) {
      this.importedBy = importedBy;
      return this;
    }

    public DependencyGraphBuilder updatedAt(String updatedAt) {
      this.updatedAt = updatedAt;
      return this;
    }

    public DependencyGraph build() {
      return new DependencyGraph(id, projectId, file, imports, importedBy, updatedAt);
    }
  }
}
