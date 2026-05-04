package com.supremeai.command;

import java.util.Map;
import java.util.HashMap;

import com.supremeai.service.ConfigService;
import com.supremeai.model.SystemConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!local")
public class AISwitchCommand implements Command {
    
    @Autowired
    private ConfigService configService;
    // This command allows for dynamic switching of AI providers.

    @Override
    public String getName() {
        return "ai-switch";
    }

    @Override
    public String getDescription() {
        return "Switch AI provider dynamically";
    }

    @Override
    public CommandCategory getCategory() {
        return CommandCategory.PROVIDER;
    }

    @Override
    public CommandType getType() {
        return CommandType.SYNC;
    }

    @Override
    public String[] getRequiredPermissions() {
        return new String[]{"system.admin"};
    }

    @Override
    public CommandSchema getSchema() {
        return new CommandSchema("ai-switch")
                .addParameter("provider", new CommandSchema.ParameterSpec("provider", String.class));
    }

    @Override
    public CommandResult execute(Map<String, Object> params, CommandContext context) {
        String provider = (String) params.get("provider");

        if (provider == null || provider.trim().isEmpty()) {
            return CommandResult.error("ai-switch", "VALIDATION_ERROR", "Provider argument is required");
        }

                // Logic to switch AI provider would go here
                // context.getEnvironment().put("supremeai.default.provider", provider);

                SystemConfig config = configService.getConfig();
                if (config != null) {
                    config.setActiveModel(provider); // Assuming provider implies the active model in this context, or add a specific provider field to SystemConfig
                    configService.updateConfig(config).subscribe();
                }

        Map<String, Object> data = new HashMap<>();
        data.put("newProvider", provider);
        data.put("status", "switched");
        data.put("message", "Switched to " + provider);

        return CommandResult.success("ai-switch", data);
    }

    @Override
    public void validate(Map<String, Object> params) {
        getSchema().validate(params);
    }
}
