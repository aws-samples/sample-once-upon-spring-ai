///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//SOURCES DiceTools.java
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M4
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M4
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

private static final Logger log = LoggerFactory.getLogger("DungeonMasterWithCustomTools");

void main() {
    log.info("=== Starting Dungeon Master AI Agent with Custom Tools ===");

    // Step 1: Create AWS Bedrock Runtime Client
    var bedrockClient = BedrockRuntimeClient.builder()
        .region(Region.US_WEST_2)
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .build();

    // Step 2: Configure model options
    var modelId = "global.anthropic.claude-haiku-4-5-20251001-v1:0";
    var options = BedrockChatOptions.builder()
        .model(modelId)
        .build();

    // Step 3: Create Spring AI ChatModel
    var chatModel = BedrockProxyChatModel.builder()
        .bedrockRuntimeClient(bedrockClient)
        .defaultOptions(options)
        .build();

    // Step 4: Build ChatClient with system prompt AND the custom dice tools
    var agent = ChatClient.builder(chatModel)
        .defaultSystem("""
            You are Lady Luck, the mystical keeper of dice and fortune in D&D adventures.
            You speak with theatrical flair and always announce dice rolls with appropriate drama.
            You know all about D&D mechanics, ability scores, and can help players with character creation.
            When rolling ability scores, remember the traditional method: roll 4d6, drop the lowest die.
            """)
        .build();

    // Step 5: Invoke the AI agent with a request that requires dice rolling
    var playerMessage = "Help me create a new D&D character! Roll the strength, wisdom, charisma and intelligence abilities scores using 4d6 drop lowest method.";
    log.info("Player: {}\n", playerMessage);

    try {
        var response = agent.prompt()
            .user(playerMessage)
            .tools(new DiceTools())
            .call()
            .content();

        log.info("Dungeon Master says:");
        log.info(response);
    } catch (Exception e) {
        log.error("Error invoking AI agent: {}", e.getMessage());
    } finally {
        log.info("\n=== Ending Dungeon Master AI Agent with Custom Tools ===");
    }
}
