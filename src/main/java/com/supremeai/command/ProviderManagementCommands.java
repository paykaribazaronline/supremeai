package com.supremeai.command;

import com.supremeai.provider.AIProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class ProviderManagementCommands {
    private static final Logger logger = LoggerFactory.getLogger(ProviderManagementCommands.class);

    private final AIProviderFactory providerFactory;

    public ProviderManagementCommands(AIProviderFactory providerFactory) {
        this.providerFactory = providerFactory;
    }

    public Command getListProvidersCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "list-providers";
            }

            @Override
            public String getDescription() {
                return "Lists all available AI providers";
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
                return new String[]{"provider.view"};
            }

            @Override
            public CommandSchema getSchema() {
                return new CommandSchema("list-providers");
            }

            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                Map<String, Object> data = new HashMap<>();
                data.put("providers", new String[]{"groq", "openai", "anthropic"});
                return CommandResult.success("list-providers", data);
            }

            @Override
            public void validate(Map<String, Object> params) {
                // No parameters
            }
        };
    }

    public Command getProviderDetailsCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "get-provider-details";
            }

            @Override
            public String getDescription() {
                return "Gets the capabilities of a specific provider";
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
                return new String[]{"provider.view"};
            }

            @Override
            public CommandSchema getSchema() {
                return new CommandSchema("get-provider-details")
                        .addParameter("provider", new CommandSchema.ParameterSpec("provider", String.class));
            }

            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                String providerName = (String) params.get("provider");
                try {
                    var provider = providerFactory.getProvider(providerName);
                    return CommandResult.success("get-provider-details", provider.getCapabilities());
                } catch (IllegalArgumentException e) {
                    return CommandResult.error("get-provider-details", "NOT_FOUND", e.getMessage());
                }
            }

            @Override
            public void validate(Map<String, Object> params) {
                getSchema().validate(params);
            }
        };
    }

    public Command getSetProviderBudgetCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "set-provider-budget";
            }

            @Override
            public String getDescription() {
                return "Sets the budget for a provider";
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
                return new String[]{"provider.admin"};
            }

            @Override
            public CommandSchema getSchema() {
                return new CommandSchema("set-provider-budget")
                        .addParameter("provider", new CommandSchema.ParameterSpec("provider", String.class))
                        .addParameter("budget", new CommandSchema.ParameterSpec("budget", Double.class));
            }

            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                String provider = (String) params.get("provider");
                Double budget = (Double) params.get("budget");
                // Mock implementation for now
                logger.info("Setting budget for {} to {}", provider, budget);
                Map<String, Object> data = new HashMap<>();
                data.put("provider", provider);
                data.put("budget", budget);
                data.put("status", "success");
                return CommandResult.success("set-provider-budget", data);
            }

            @Override
            public void validate(Map<String, Object> params) {
                getSchema().validate(params);
            }
        };
    }
}
