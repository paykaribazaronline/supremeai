package com.supremeai.plugin;

import org.springframework.stereotype.Component;
import java.util.*;
import java.nio.file.*;

/**
 * Plugin System - Plan 24 Week 13-14
 * Ruflo-style plugin architecture
 */
@Component
public class PluginManager {
    private final Map<String, Plugin> plugins = new HashMap<>();
    private final Path pluginDir = Paths.get("plugins");
    
    /**
     * Install plugin from marketplace
     */
    public Plugin installPlugin(String pluginId) {
        // TODO: Download from marketplace
        Plugin plugin = new Plugin();
        plugin.id = pluginId;
        plugin.name = pluginId;
        plugin.installed = true;
        plugin.enabled = true;
        
        plugins.put(pluginId, plugin);
        return plugin;
    }
    
    /**
     * List all installed plugins
     */
    public List<Plugin> listPlugins() {
        return new ArrayList<>(plugins.values());
    }
    
    /**
     * Enable/disable plugin
     */
    public void setPluginEnabled(String pluginId, boolean enabled) {
        Plugin plugin = plugins.get(pluginId);
        if (plugin != null) {
            plugin.enabled = enabled;
        }
    }
    
    /**
     * Plugin info
     */
    public static class Plugin {
        public String id;
        public String name;
        public String version = "1.0.0";
        public boolean installed;
        public boolean enabled;
        public String description;
    }
}
