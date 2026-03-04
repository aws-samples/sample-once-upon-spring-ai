package com.amazonaws;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

record InquireRequest(String question) {}

record DiceOutput(String diceType, String result, String reason) {}

record StoryOutput(
    String response,
    List<String> actionsSuggestions,
    String details,
    List<DiceOutput> diceRolls
) {}

/// REST controller — the main API the frontend or user interacts with.
@RestController
@CrossOrigin(origins = "*")
class GameMasterController {

    private static final Logger log = LoggerFactory.getLogger("GameMasterController");

    private final ChatClient chatClient;
    private final GameMasterService remoteAgent;
    private final ToolCallback[] mcpTools;
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<Map<String, String>> conversationHistory =
            Collections.synchronizedList(new ArrayList<>());

    GameMasterController(ChatClient chatClient, GameMasterService remoteAgent,
                         ToolCallback[] mcpTools) {
        this.chatClient = chatClient;
        this.remoteAgent = remoteAgent;
        this.mcpTools = mcpTools;
    }

    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "healthy", "agent", "gamemaster-orchestrator");
    }

    @GetMapping("/messages")
    List<Map<String, String>> getMessages() {
        return List.copyOf(conversationHistory);
    }

    @GetMapping("/user/{userName}")
    Object getUser(@PathVariable String userName) {
        try {
            var file = new File("./../character/characters.json");
            if (!file.exists()) return Map.of("error", "Character database not found");

            List<Map<String, Object>> characters = mapper.readValue(
                    file, new TypeReference<List<Map<String, Object>>>() {});

            return characters.stream()
                    .filter(c -> userName.equalsIgnoreCase((String) c.get("name")))
                    .findFirst()
                    .orElse(Map.of("error", "Character with name '%s' not found".formatted(userName)));
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/inquire")
    Map<String, Object> inquire(@RequestBody InquireRequest request) {
        log.info("Processing request: {}", request.question());
        conversationHistory.add(Map.of("role", "user", "content", request.question()));

        try {
            var content = chatClient.prompt()
                    .user(request.question())
                    .tools(remoteAgent)
                    .toolCallbacks(mcpTools)
                    .call()
                    .content();

            log.info("Orchestrator response ready");

            try {
                var storyOutput = mapper.readValue(content, StoryOutput.class);
                conversationHistory.add(Map.of("role", "assistant", "content", storyOutput.response()));
                return Map.of("response", storyOutput);
            } catch (Exception parseEx) {
                var fallback = new StoryOutput(content, List.of(), "Direct response", List.of());
                conversationHistory.add(Map.of("role", "assistant", "content", content));
                return Map.of("response", fallback);
            }
        } catch (Exception e) {
            log.error("Error processing request: {}", e.getMessage());
            return Map.of("error", "Internal server error: " + e.getMessage());
        }
    }
}
