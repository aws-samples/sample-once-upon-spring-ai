///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M4
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M4
//DEPS org.springaicommunity:spring-ai-agent-utils:0.5.0
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import org.springaicommunity.agent.tools.SmartWebFetchTool;

private static final Logger log = LoggerFactory.getLogger("DungeonMasterWithBuiltInTools");

void main() {
    log.info("=== Starting Dungeon Master AI Agent with Built-in Tools ===");

    // Step 1: Read the Bedrock API key from environment
    var bearerToken = System.getenv("AWS_BEARER_TOKEN_BEDROCK");
    if (bearerToken == null || bearerToken.isBlank()) {
        log.error("Set AWS_BEARER_TOKEN_BEDROCK first — get your key from the Amazon Bedrock Console → API keys → Short-term API keys");
        return;
    }

    // Step 2: Create AWS Bedrock Runtime Client with API key (bearer token auth)
    var bedrockClient = BedrockRuntimeClient.builder()
        .region(Region.US_WEST_2)
        .credentialsProvider(AnonymousCredentialsProvider.create())
        .overrideConfiguration(c -> c.putHeader("Authorization", "Bearer " + bearerToken))
        .build();

    // Step 2: Configure model options
    var modelId = "us.anthropic.claude-haiku-4-5-20251001-v1:0";
    var options = BedrockChatOptions.builder()
        .model(modelId)
        .build();

    // Step 3: Create Spring AI ChatModel and its associated AI Agent
    var chatModel = BedrockProxyChatModel.builder()
        .bedrockRuntimeClient(bedrockClient)
        .defaultOptions(options)
        .build();
    var agent = ChatClient.builder(chatModel).build();

    // Step 4: Create SmartWebFetchTool tool to enable the AI agent to fetch / process web content
    var webFetchTool = SmartWebFetchTool.builder(agent)
        .maxContentLength(300_000)
        .build();

    try {
        // Step 5: Ask the AI to fetch and extract information from Wikipedia
        var response = agent.prompt()
            .user("Using the website https://en.wikipedia.org/wiki/Dungeons_%26_Dragons tell me the name of the designers of Dungeons and Dragons.")
            .tools(webFetchTool)
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
