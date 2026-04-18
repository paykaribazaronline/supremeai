package com.supremeai.command;

import java.util.Map;
import java.util.HashMap;

public class AISwitchCommand implements Command {

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
