package org.example.model;

import java.io.Serializable;
import java.util.*;

/**
 * Improve Model
 * Generated: 2026-04-08T16:28:38.279024307
 */
public class Improve implements Serializable {
    private String id;
    private String name;
    private Map<String, Object> data = new HashMap<>();
    private long timestamp = System.currentTimeMillis();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Map<String, Object> getData() { return data; }
    public void setData(Map<String, Object> data) { this.data = data; }

    public long getTimestamp() { return timestamp; }

}
