///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M2
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M2
// TODO 1: Add the Spring AI Community agent-utils dependency that provides SmartWebFetchTool.
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

// TODO 2: Import the SmartWebFetchTool class from the community library.

private static final Logger log = LoggerFactory.getLogger("DungeonMasterWithBuiltInTools");

void main() {
    log.info("=== Starting Dungeon Master AI Agent with Built-in Tools ===");

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

    // Step 3: Create Spring AI ChatModel and its associated AI Agent
    var chatModel = BedrockProxyChatModel.builder()
        .bedrockRuntimeClient(bedrockClient)
        .defaultOptions(options)
        .build();
    var agent = ChatClient.builder(chatModel).build();

    // TODO 3: Create a SmartWebFetchTool and use it to equip the agent.
    //   1. Build the tool:     var webFetchTool = SmartWebFetchTool.builder(agent).maxContentLength(300_000).build();
    //   2. Use it in a prompt: agent.prompt().user("...").tools(webFetchTool).call().content();
    //
    //   Ask the AI: "Using the website https://en.wikipedia.org/wiki/Dungeons_%26_Dragons
    //               tell me the name of the designers of Dungeons and Dragons."
    //
    //   The SmartWebFetchTool lets the AI autonomously fetch and parse web pages — giving
    //   your agent the ability to access real-time information from the internet.

    try {
        var response = agent.prompt()
            .user("Using the website https://en.wikipedia.org/wiki/Dungeons_%26_Dragons tell me the name of the designers of Dungeons and Dragons.")
            // TODO 3 (continued): Add .tools(webFetchTool) here to give the agent web fetching ability
            .call()
            .content();

        log.info("Agent Response:");
        log.info(response);
    } catch (Exception e) {
        log.error("Error invoking AI agent: {}", e.getMessage());
    } finally {
        log.info("\n=== Ending Dungeon Master AI Agent with Built-in Tools ===");
    }
}
