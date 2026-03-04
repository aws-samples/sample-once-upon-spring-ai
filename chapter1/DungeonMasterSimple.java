///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M2
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M2
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

    // Step 1: Create AWS Bedrock Runtime Client
    var bedrockClient = BedrockRuntimeClient.builder()
        .region(Region.US_WEST_2)
        .credentialsProvider(DefaultCredentialsProvider.builder().build())
        .build();

    // Step 2: Configure model options (which Claude model to use)
    var modelId = "global.anthropic.claude-haiku-4-5-20251001-v1:0";
    var options = BedrockChatOptions.builder()
        .model(modelId)
        .build();

    // Step 3: Create Spring AI ChatModel (wraps Bedrock client)
    var chatModel = BedrockProxyChatModel.builder()
        .bedrockRuntimeClient(bedrockClient)
        .defaultOptions(options)
        .build();

    // TODO 2: Build a ChatClient (the "agent") with a system prompt that sets the AI personality.
    //   Use ChatClient.builder(chatModel) to create the builder.
    //   Chain .defaultSystem("You are a game master for a Dungeon & Dragon game") to set the persona.
    //   Call .build() to finalize.
    //   Store the result in a variable called 'agent'.

    // TODO 3: Send a message to the agent and print the response.
    //   1. Define a player message, e.g. "Hi, I am an adventurer ready for adventure!"
    //   2. Call agent.prompt().user(playerMessage).call().content() to get the AI response
    //   3. Log the response using log.info()
    //   4. Wrap the call in a try/catch block to handle any exceptions
    //
    //   Example pattern:
    //     try {
    //         var response = agent.prompt().user(playerMessage).call().content();
    //         log.info("Dungeon Master says:");
    //         log.info(response);
    //     } catch (Exception e) {
    //         log.error("Error invoking AI agent: {}", e.getMessage());
    //     }

    log.info("\n=== Ending Dungeon Master AI Agent ===");
}
