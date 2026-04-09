///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//SOURCES CharacterTools.java
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.boot:spring-boot-starter-web:4.0.2
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M4
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M4
//DEPS org.springaicommunity:spring-ai-a2a-server-autoconfigure:0.2.0
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34
//DEPS com.fasterxml.jackson.core:jackson-databind:2.18.4
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.springframework:spring-core:7.0.3

package com.amazonaws;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentSkill;

import org.springaicommunity.a2a.server.executor.DefaultAgentExecutor;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.chat.client.ChatClient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Value;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import java.util.List;
import java.util.Map;

/// Character Agent — D&D character management via A2A protocol.
/// Uses Spring AI A2A server autoconfiguration for agent discovery and communication.
/// Run with: jbang CharacterAgent.java
@SpringBootApplication
public class CharacterAgent {

    private static final String SYSTEM_PROMPT = """
        You are a D&D character management specialist. You handle character creation, lookup, and inventory.
        When creating characters, always roll ability scores using the traditional method: roll 4d6 and drop
        the lowest die for each of the six abilities (Strength, Dexterity, Constitution, Intelligence, Wisdom, Charisma).
        Use the appropriate tools to create, find, or list characters as requested.
        When finding a character, always include their full stats, level, XP, AND inventory in the response.
        For inventory management, use addInventoryItem and removeInventoryItem to track equipment, loot, and gold.
        Keep responses focused and always include ALL character details: class, race, stats, and inventory.
        """;

    public static void main(final String[] args) {
        // TODO 1: Configure Spring Boot properties for this A2A agent server.
        //   Set these system properties before calling SpringApplication.run():
        //     - "server.port"                  → "8001"     (Character Agent runs on port 8001)
        //     - "server.servlet.context-path"  → "/a2a"     (same A2A path convention)
        //     - "spring.application.name"      → "character-agent"
        //     - "spring.ai.a2a.server.enabled" → "true"
        //   If you completed the Rules Agent, this follows the exact same pattern!

        SpringApplication.run(CharacterAgent.class, args);
    }

    // TODO 2: Create an AgentCard bean describing this agent's character management capabilities.
    //   Follow the same pattern as the Rules Agent, but with character-specific skills:
    //
    //   @Bean
    //   AgentCard agentCard(@Value("${server.port:8001}") int port,
    //                       @Value("${server.servlet.context-path:/a2a}") String contextPath) {
    //       return new AgentCard.Builder()
    //               .name("Character Agent")
    //               .description("Specialized D&D character management agent...")
    //               .url("http://localhost:" + port + contextPath + "/")
    //               .version("1.0.0")
    //               .capabilities(new AgentCapabilities.Builder().streaming(false).build())
    //               .defaultInputModes(List.of("text"))
    //               .defaultOutputModes(List.of("text"))
    //               .skills(List.of(
    //                   new AgentSkill.Builder()
    //                       .id("create_character").name("Create Character")
    //                       .description("Create a new D&D character with rolled ability scores")
    //                       .tags(List.of("character", "creation", "dnd"))
    //                       .examples(List.of("Create a female Elf Wizard named Lyria"))
    //                       .build(),
    //                   new AgentSkill.Builder()
    //                       .id("find_character").name("Find Character")
    //                       .description("Find an existing character by name — returns full stats, level, XP, and inventory")
    //                       .tags(List.of("character", "lookup", "inventory"))
    //                       .examples(List.of("Find the character named Ragnar"))
    //                       .build(),
    //                   new AgentSkill.Builder()
    //                       .id("list_characters").name("List Characters")
    //                       .description("List all characters in the database with their inventory")
    //                       .tags(List.of("character", "list"))
    //                       .examples(List.of("List all characters"))
    //                       .build(),
    //                   new AgentSkill.Builder()
    //                       .id("add_inventory").name("Add Inventory Item")
    //                       .description("Add an item to a character's inventory — use for loot, purchases, or quest rewards")
    //                       .tags(List.of("character", "inventory", "loot"))
    //                       .examples(List.of("Add a Longsword to Ragnar's inventory", "Give Lyria 50 gold pieces"))
    //                       .build(),
    //                   new AgentSkill.Builder()
    //                       .id("remove_inventory").name("Remove Inventory Item")
    //                       .description("Remove an item from a character's inventory — use when items are consumed, sold, or lost")
    //                       .tags(List.of("character", "inventory"))
    //                       .examples(List.of("Remove a health potion from Ragnar's inventory"))
    //                       .build()))
    //               .protocolVersion("0.3.0")
    //               .build();
    //   }

    // TODO 3: Create an AgentExecutor bean that wires the ChatClient with the CharacterTools.
    //   Same pattern as the Rules Agent, but using CharacterTools instead of RulesTools:
    //
    //   @Bean
    //   AgentExecutor agentExecutor(BedrockProxyChatModel chatModel, CharacterTools characterTools) {
    //       var chatClient = ChatClient.builder(chatModel)
    //               .defaultSystem(SYSTEM_PROMPT)
    //               .defaultTools(characterTools)
    //               .build();
    //
    //       return new DefaultAgentExecutor(chatClient, (chat, requestContext) -> {
    //           String userMessage = DefaultAgentExecutor.extractTextFromMessage(requestContext.getMessage());
    //           return chat.prompt().user(userMessage).call().content();
    //       });
    //   }
}

/// Bedrock ChatModel configuration
@Configuration
class CharacterAgentConfig {

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
                .build();
    }
}

/// Health check endpoint
@RestController
class CharacterAgentController {

    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", "healthy", "agent", "character-agent");
    }
}
