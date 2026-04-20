package com.supremeai.controller;

import com.supremeai.command.CommandContext;
import com.supremeai.command.CommandExecutor;
import com.supremeai.command.CommandResult;
import com.supremeai.model.User;
import com.supremeai.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/commands")
public class CommandController {

    private final CommandExecutor executor;
    private final UserRepository userRepository;

    public CommandController(CommandExecutor executor, UserRepository userRepository) {
        this.executor = executor;
        this.userRepository = userRepository;
    }

    @PostMapping("/execute")
    @SuppressWarnings("unchecked")
    public CommandResult execute(@RequestBody Map<String, Object> request, Authentication authentication, HttpServletRequest httpRequest) {
        String commandName = (String) request.get("name");
        Map<String, Object> parameters = (Map<String, Object>) request.get("parameters");

        if (authentication == null || !authentication.isAuthenticated()) {
            return CommandResult.error(commandName, "AUTH_ERROR", "User is not authenticated.");
        }

        String uid = authentication.getName();
        User user = userRepository.findByFirebaseUid(uid).block();
        if (user == null) {
            throw new IllegalStateException("Authenticated user not found in database.");
        }

        String[] roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toArray(String[]::new);

        CommandContext context = new CommandContext(uid, user.getDisplayName(), roles);
        context.setSourceIp(httpRequest.getRemoteAddr());
        context.setSourceApp("API");

        return executor.execute(commandName, parameters, context);
    }

    @GetMapping("/list")
    public Map<String, Object> list() {
        return Map.of(
                "commands", executor.listCommands().stream()
                        .map(c -> Map.of("name", c.getName(), "description", c.getDescription()))
                        .collect(Collectors.toList())
        );
    }

    @GetMapping("/{name}")
    public Map<String, Object> getCommandDetails(@PathVariable String name) {
        com.supremeai.command.Command command = executor.getCommand(name);
        if (command == null) {
            return Map.of("error", "Command not found");
        }
        return Map.of(
                "name", command.getName(),
                "description", command.getDescription(),
                "parameters", command.getSchema().getParameters().entrySet().stream()
                        .map(entry -> Map.of(
                                "name", entry.getKey(),
                                "type", entry.getValue().getType().getSimpleName()
                        ))
                        .collect(Collectors.toList())
        );
    }
}