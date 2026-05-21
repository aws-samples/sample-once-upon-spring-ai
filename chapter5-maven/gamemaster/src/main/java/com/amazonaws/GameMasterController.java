package com.amazonaws;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

record InquireRequest(String question) {}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record DiceOutput(String diceType, String result, String reason) {}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record StoryOutput(
    String response,
    List<String> actionsSuggestions,
    String details,
    List<DiceOutput> dicesRolls
) {}

/// REST controller — the main API the frontend or user interacts with.
/// Uses `originPatterns = "*"` (not `origins = "*"`) so this works with
/// any host the workshop is deployed behind — localhost, `.localhost` cloud
/// IDEs, DuckDNS subdomains, Gitpod, GitHub Codespaces, etc. Unlike
/// `origins = "*"`, `originPatterns` echoes the request's Origin back and
/// is compatible with credentialed requests, so nothing silently breaks
/// when auth is added later.
@RestController
@CrossOrigin(originPatterns = "*")
class GameMasterController {

    private static final Logger log = LoggerFactory.getLogger("GameMasterController");

    private final ChatClient chatClient;
    private final GameMasterService remoteAgent;
    private final ToolCallback[] mcpTools;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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
            var result = remoteAgent.sendMessage("Character Agent", """
                    Find the character named %s using the findCharacterByName tool.
                    Respond with ONLY the raw JSON object returned by the tool — no prose,
                    no markdown code fences, no commentary. The response MUST start with {
                    and end with }, and MUST include these snake_case fields:
                    character_id, name, character_class, race, gender, level, experience,
                    stats (with strength, dexterity, constitution, intelligence, wisdom,
                    charisma), inventory, created_at.
                    """.formatted(userName));

            var start = result.indexOf('{');
            var end = result.lastIndexOf('}');
            if (start < 0 || end <= start) {
                log.warn("Character Agent did not return JSON for '{}': {}", userName, result);
                return Map.of("error", "Character not found or response unparseable");
            }
            return mapper.readValue(result.substring(start, end + 1), Map.class);
        } catch (Exception e) {
            log.error("Error fetching user '{}'", userName, e);
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
                var cleaned = content
                        .replaceAll("```json\\s*", "")
                        .replaceAll("```", "")
                        .trim();
                var storyOutput = mapper.readValue(cleaned, StoryOutput.class);
                conversationHistory.add(Map.of("role", "assistant", "content", storyOutput.response()));
                return Map.of("response", storyOutput);
            } catch (Exception _) {
                var fallback = new StoryOutput(content, List.of(), "Direct response", List.of());
                conversationHistory.add(Map.of("role", "assistant", "content", content));
                return Map.of("response", fallback);
            }
        } catch (Exception e) {
            log.error("Error processing request", e);
            return Map.of("error", "Internal server error: " + e.getMessage());
        }
    }
}
