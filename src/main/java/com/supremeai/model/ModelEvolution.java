package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.List;

@Document(collectionName = "model_evolution")
public class ModelEvolution {
    @DocumentId
    private String id;
    private String name;
    private Integer level;
    private Integer xp;
    private List<String> contributions;
    private Boolean isAlphaPlayer;

    public ModelEvolution() {}

    public ModelEvolution(String id, String name, Integer level, Integer xp, List<String> contributions, Boolean isAlphaPlayer) {
        this.id = id;
        this.name = name;
        this.level = level;
        this.xp = xp;
        this.contributions = contributions;
        this.isAlphaPlayer = isAlphaPlayer;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getLevel() { return level; }
    public void setLevel(Integer level) { this.level = level; }
    public Integer getXp() { return xp; }
    public void setXp(Integer xp) { this.xp = xp; }
    public List<String> getContributions() { return contributions; }
    public void setContributions(List<String> contributions) { this.contributions = contributions; }
    public Boolean getIsAlphaPlayer() { return isAlphaPlayer; }
    public void setIsAlphaPlayer(Boolean alphaPlayer) { isAlphaPlayer = alphaPlayer; }
}
