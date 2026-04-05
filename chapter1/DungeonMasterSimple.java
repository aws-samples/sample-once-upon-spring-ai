///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M4
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M4
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34
//DEPS org.slf4j:slf4j-api:2.0.17

// TODO 1: Add the SLF4J simple logging dependency so you can see what the agent is thinking


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import org.springframework.ai.chat.client.ChatClient;

private static final Logger log = LoggerFactory.getLogger("DungeonMasterSimple");

void main() {
    log.info("=== Starting Dungeon Master AI Agent ===");

    var bedrockClient = BedrockRuntimeClient.builder()
        .region(Region.US_WEST_2)
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .build();

    var modelId = "global.anthropic.claude-haiku-4-5-20251001-v1:0";
    var options = BedrockChatOptions.builder()
        .model(modelId)
        .build();

    var chatModel = BedrockProxyChatModel.builder()
        .bedrockRuntimeClient(bedrockClient)
        .defaultOptions(options)
        .build();

    // TODO 2: Build a ChatClient with a system prompt that sets the AI personality


    // TODO 3: Send a message to the agent and print the response
    

    log.info("\n=== Ending Dungeon Master AI Agent ===");
}
