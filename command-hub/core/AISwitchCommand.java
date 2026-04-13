package org.example.command;

import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.HashMap;

@Component
@Command(name = "ai-switch", description = "Switch AI provider dynamically")
public class AISwitchCommand implements CommandExecutor {

    @Override
    public CommandResult execute(CommandContext context) {
        String provider = (String) context.getParameters().get("provider");
        
        if (provider == null || provider.trim().isEmpty()) {
            return CommandResult.error("ai-switch", "Provider argument is required");
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("newProvider", provider);
        data.put("status", "switched");
        data.put("message", "Switched to " + provider);
        
        return CommandResult.success("ai-switch", data);
    }
}
