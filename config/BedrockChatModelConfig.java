///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//SOURCES SanitizingToolCallingManager.java
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M4
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

public class BedrockChatModelConfig {

    private static final Logger log = LoggerFactory.getLogger("BedrockChatModelConfig");

    private static String getBearerToken() {
        var bearerToken = System.getenv("AWS_BEARER_TOKEN_BEDROCK");
        if (bearerToken == null || bearerToken.isBlank()) {
            log.error("Set AWS_BEARER_TOKEN_BEDROCK first — get your key from the Amazon Bedrock Console → API keys → Short-term API keys");
            throw new IllegalStateException("Missing AWS_BEARER_TOKEN_BEDROCK environment variable");
        }
        return bearerToken;
    }

    public static BedrockProxyChatModel createChatModel() {
        // Step 1: Read the Bedrock API key from environment
        var bearerToken = getBearerToken();

        // Step 2: Create AWS Bedrock Runtime Client with API key (bearer token auth)
        var bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(AnonymousCredentialsProvider.create())
            .overrideConfiguration(c -> c.putHeader("Authorization", "Bearer " + bearerToken))
            .build();

        // Step 3: Configure model options (which Claude model to use)
        var modelId = "us.anthropic.claude-haiku-4-5-20251001-v1:0";
        var options = BedrockChatOptions.builder()
            .model(modelId)
            .build();

        // Step 4: Create Spring AI ChatModel (wraps Bedrock client)
        return BedrockProxyChatModel.builder()
            .bedrockRuntimeClient(bedrockClient)
            .defaultOptions(options)
            .build();
    }

    public static BedrockProxyChatModel createChatModelWithSanitizingToolCallingManager() {
        var bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();

        var options = BedrockChatOptions.builder()
                .model("global.anthropic.claude-haiku-4-5-20251001-v1:0")
                .build();

        return BedrockProxyChatModel.builder()
                .bedrockRuntimeClient(bedrockClient)
                .defaultOptions(options)
                .toolCallingManager(new SanitizingToolCallingManager())
                .build();
    }
}