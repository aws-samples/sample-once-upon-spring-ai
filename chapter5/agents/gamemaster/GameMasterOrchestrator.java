///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//SOURCES GameMasterService.java
//SOURCES GameMasterController.java
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.boot:spring-boot-starter-web:4.0.2
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M4
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M4
//DEPS org.springframework.ai:spring-ai-mcp:2.0.0-M4
//DEPS io.github.a2asdk:a2a-java-sdk-client:0.3.3.Final
//DEPS io.github.a2asdk:a2a-java-sdk-client-transport-jsonrpc:0.3.3.Final
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.4
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.springframework:spring-core:7.0.3

package com.amazonaws;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

/// Game Master Orchestrator — coordinates Rules Agent, Character Agent via A2A, and MCP Dice Server.
///
/// Architecture:
///   GameMasterOrchestrator (port 8009)
///       ├── Rules Agent      (A2A → port 8000)
///       ├── Character Agent   (A2A → port 8001)
///       └── MCP Dice Server   (MCP → port 8080)
///
/// Run with: jbang GameMasterOrchestrator.java
@SpringBootApplication
public class GameMasterOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(GameMasterOrchestrator.class);

    private static final String SYSTEM_PROMPT = """
        You are the Grand Game Master — the supreme narrator of D&D adventures.
        You have specialized agents and tools at your disposal to gather information,
        but YOUR voice is what the player hears. You MUST synthesize all agent and tool
        results into your own dramatic, immersive Game Master narrative.

        === AVAILABLE AGENTS (use sendMessage to communicate) ===
        %s

        === HOW TO USE AGENTS AND TOOLS ===
        1. Use sendMessage with the EXACT agent name and a clear task description.
        2. Use rollDice (via MCP) for dice rolling — available types: d4, d6, d8, d10, d12, d20, d100.

        === MANDATORY GAMEPLAY WORKFLOW ===
        IMPORTANT: You MUST use your tools and agents proactively. NEVER ask the player for information
        that you can retrieve yourself. If the player mentions a character name, immediately look it up.
        If an action requires a dice roll, roll the dice — don't ask if the player wants to roll.
        If a rule is relevant, look it up — don't guess from memory.

        When a player is created or referenced:
        1. ALWAYS use the Character Agent (sendMessage → "Character Agent") to retrieve full character details.
        2. Use the retrieved stats, class, race, level, and inventory to personalize every response.
        3. If you don't know the character name, use the Character Agent to list all characters first.

        During gameplay — you MUST act, not ask:
        - Use the Character Agent to look up character stats for ability checks, saves, and attacks.
        - Use the Rules Agent to look up D&D rules for combat, spellcasting, or skill checks.
        - Use rollDice for EVERY dice roll (attack, damage, ability check, saving throw) — roll immediately.
        - Combine multiple agent/tool calls in a single turn (e.g., look up a rule AND roll dice AND check stats).

        === RESPONSE RULES ===
        - Tool and agent results are RAW DATA — NEVER pass them through verbatim.
        - ALWAYS rewrite and enrich responses in your own dramatic Game Master voice.
        - When the Rules Agent provides a rule, explain it narratively with examples.
        - When the Character Agent returns character data, weave it into the story.
        - When dice are rolled, narrate the tension and outcome theatrically.
        - Always use the sendMessage tool with the exact agent name. Never invent or guess URLs.

        === OUTPUT FORMAT ===
        Always respond as JSON (no markdown fences) with these fields:
        - "response": Your synthesized narrative response as Game Master
        - "actions_suggestions": A list of 3 suggested actions for the player
        - "details": Brief summary of which tools/agents were consulted
        - "dices_rolls": A list of dice rolls, each with "dice_type", "result", and "reason"
        """;

    public static void main(final String[] args) {
        System.setProperty("server.port", "8009");
        System.setProperty("spring.application.name", "gamemaster-orchestrator");
        // TODO 1: Set the remote.agents.urls property with the A2A agent URLs
        //   The orchestrator needs to know where the Rules Agent and Character Agent are running.
        //   At startup, GameMasterService will fetch each agent's
        //   card from /.well-known/agent-card.json and register them by name.

        SpringApplication.run(GameMasterOrchestrator.class, args);
    }

    @Bean
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder().maxMessages(20).build();
    }

    // TODO 3: Build the ChatClient bean that the orchestrator uses to process user requests.
    //   The system prompt includes a %s placeholder that gets filled with the discovered agent descriptions.
    //   Also wire in the ChatMemory via MessageChatMemoryAdvisor so the orchestrator remembers conversation context.
    //
    //   @Bean
    //   ChatClient chatClient(BedrockProxyChatModel chatModel,
    //                          GameMasterService remoteAgent,
    //                          ChatMemory chatMemory) {
    //       var systemPrompt = SYSTEM_PROMPT.formatted(remoteAgent.getAgentDescriptions());
    //       log.info("Initializing routing ChatClient with agents: {}", remoteAgent.getAgentNames());
    //       return ChatClient.builder(chatModel)
    //               .defaultSystem(systemPrompt)
    //               .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
    //               .build();
    //   }
}

/// CORS configuration
@Configuration
class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST")
                .allowedHeaders("*");
    }
}

/// MCP client connection to the Dice Roll Server from chapter4.
/// Connects lazily — the orchestrator starts even if the dice server is down.
@Configuration
class McpClientConfig {

    private static final Logger log = LoggerFactory.getLogger("McpClientConfig");

    // TODO 2: Create an MCP client bean that connects to the Dice Roll Server on port 8080
    //   and returns the tools as Spring AI ToolCallback[].
    //
    //   @Bean
    //   ToolCallback[] mcpTools() {
    //       try {
    //           var transport = HttpClientStreamableHttpTransport.builder("http://localhost:8080")
    //                   .endpoint("/mcp")
    //                   .build();
    //           var client = McpClient.sync(transport)
    //                   .clientInfo(new McpSchema.Implementation("gamemaster-mcp-client", "1.0.0"))
    //                   .build();
    //           client.initialize();
    //           var tools = client.listTools().tools().stream().map(McpSchema.Tool::name).toList();
    //           log.info("MCP tools discovered: {}", tools);
    //           return SyncMcpToolCallbackProvider.builder()
    //                   .mcpClients(client)
    //                   .build()
    //                   .getToolCallbacks();
    //       } catch (Exception e) {
    //           log.warn("MCP Dice Server not available — dice rolling disabled");
    //           log.warn("Start it with: cd ../chapter4 && jbang DiceRollMcpServer.java");
    //           return new ToolCallback[0];
    //       }
    //   }
}

/// Spring AI Bedrock ChatModel configuration
@Configuration
class ChatModelConfig {

    @Bean
    BedrockProxyChatModel chatModel() {
        var bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();

        var options = BedrockChatOptions.builder()
                .model("us.anthropic.claude-haiku-4-5-20251001-v1:0")
                .build();

        return BedrockProxyChatModel.builder()
                .bedrockRuntimeClient(bedrockClient)
                .defaultOptions(options)
                .toolCallingManager(new SanitizingToolCallingManager())
                .build();
    }
}

/// Wraps DefaultToolCallingManager to escape literal control characters in LLM-generated
/// tool call arguments. Bedrock models sometimes emit unescaped newlines/tabs inside JSON
/// string values, which causes Jackson to reject them when BedrockProxyChatModel re-parses
/// tool arguments during the conversation loop in createRequest().
class SanitizingToolCallingManager implements ToolCallingManager {

    private final DefaultToolCallingManager delegate = DefaultToolCallingManager.builder().build();

    @Override
    public List<ToolDefinition> resolveToolDefinitions(ToolCallingChatOptions chatOptions) {
        return delegate.resolveToolDefinitions(chatOptions);
    }

    @Override
    public ToolExecutionResult executeToolCalls(Prompt prompt, ChatResponse chatResponse) {
        return delegate.executeToolCalls(prompt, sanitizeToolCallArgs(chatResponse));
    }

    private ChatResponse sanitizeToolCallArgs(ChatResponse response) {
        var generations = response.getResults().stream().map(gen -> {
            var output = gen.getOutput();
            if (!output.hasToolCalls()) return gen;

            var sanitizedCalls = output.getToolCalls().stream()
                    .map(tc -> new AssistantMessage.ToolCall(
                            tc.id(), tc.type(), tc.name(),
                            escapeControlChars(tc.arguments())))
                    .toList();

            var sanitizedMessage = AssistantMessage.builder()
                    .content(output.getText())
                    .properties(output.getMetadata())
                    .toolCalls(sanitizedCalls)
                    .media(output.getMedia())
                    .build();
            return new Generation(sanitizedMessage, gen.getMetadata());
        }).toList();

        return new ChatResponse(generations, response.getMetadata());
    }

    /// Escapes literal control characters inside JSON string values.
    static String escapeControlChars(String json) {
        var sb = new StringBuilder(json.length());
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < json.length(); i++) {
            var ch = json.charAt(i);
            if (escaped) {
                sb.append(ch);
                escaped = false;
                continue;
            }
            if (ch == '\\' && inString) {
                sb.append(ch);
                escaped = true;
                continue;
            }
            if (ch == '"') {
                inString = !inString;
                sb.append(ch);
                continue;
            }
            if (inString && ch < 0x20) {
                switch (ch) {
                    case '\n' -> sb.append("\\n");
                    case '\r' -> sb.append("\\r");
                    case '\t' -> sb.append("\\t");
                    default -> sb.append("\\u%04x".formatted((int) ch));
                }
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
