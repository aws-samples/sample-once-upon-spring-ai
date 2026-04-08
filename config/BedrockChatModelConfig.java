///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M2
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34

import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

public class BedrockChatModelConfig {

    public static BedrockProxyChatModel createChatModel() {
        var bedrockClient = BedrockRuntimeClient.builder()
            .region(Region.US_WEST_2)
            .credentialsProvider(DefaultCredentialsProvider.builder().build())
            .build();

        var modelId = "us.anthropic.claude-haiku-4-5-20251001-v1:0";
        var options = BedrockChatOptions.builder()
            .model(modelId)
            .build();

        return BedrockProxyChatModel.builder()
            .bedrockRuntimeClient(bedrockClient)
            .defaultOptions(options)
            .build();
    }

}
