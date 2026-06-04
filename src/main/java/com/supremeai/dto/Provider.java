package com.supremeai.dto;

public class Provider {
  private String id;
  private String name;
  private String type;
  private boolean active;

  public Provider() {}

  public Provider(String id, String name, String type, boolean active) {
    this.id = id;
    this.name = name;
    this.type = type;
    this.active = active;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public static ProviderBuilder builder() {
    return new ProviderBuilder();
  }

  public static class ProviderBuilder {
    private String id;
    private String name;
    private String type;
    private boolean active;

    ProviderBuilder() {}

    public ProviderBuilder id(String id) {
      this.id = id;
      return this;
    }

    public ProviderBuilder name(String name) {
      this.name = name;
      return this;
    }

    public ProviderBuilder type(String type) {
      this.type = type;
      return this;
    }

    public ProviderBuilder active(boolean active) {
      this.active = active;
      return this;
    }

    public Provider build() {
      return new Provider(id, name, type, active);
    }
  }
}
