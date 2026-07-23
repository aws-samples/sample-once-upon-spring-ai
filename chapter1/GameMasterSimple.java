///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS io.netty:netty-bom:4.2.9.Final@pom
//DEPS tools.jackson:jackson-bom:3.1.4@pom
//DEPS com.fasterxml.jackson.core:jackson-annotations:2.21
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//RUNTIME_OPTIONS -Daws.region=us-west-2 --enable-native-access=ALL-UNNAMED

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

import org.springframework.ai.chat.client.ChatClient;

private static final Logger log = LoggerFactory.getLogger("GameMasterSimple");

void main() {
    log.info("=== Starting Game Master AI Agent ===");

    var bearerToken = System.getenv("AWS_BEARER_TOKEN_BEDROCK");
    if (bearerToken == null || bearerToken.isBlank()) {
        log.error("Set AWS_BEARER_TOKEN_BEDROCK first — get your key from the Amazon Bedrock Console → API keys → Short-term API keys");
        return;
    }

    var bedrockClient = BedrockRuntimeClient.builder()
        .region(Region.US_WEST_2)
        .credentialsProvider(AnonymousCredentialsProvider.create())
        .overrideConfiguration(c -> c.putHeader("Authorization", "Bearer " + bearerToken))
        .build();

    var modelId = "us.anthropic.claude-haiku-4-5-20251001-v1:0";
    var options = BedrockChatOptions.builder()
        .model(modelId)
        .build();

    var chatModel = BedrockProxyChatModel.builder()
        .bedrockRuntimeClient(bedrockClient)
        .options(options)
        .build();

    // TODO 1: Build a ChatClient with a system prompt that sets the AI personality
    

    // TODO 2: Send a message to the agent and print the response
    

    log.info("\n=== Ending Game Master AI Agent ===");
}
