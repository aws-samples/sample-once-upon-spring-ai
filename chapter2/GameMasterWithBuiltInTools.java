///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral
//DEPS io.netty:netty-bom:4.2.21.Final@pom
//DEPS tools.jackson:jackson-bom:3.1.8@pom
//DEPS org.springframework:spring-framework-bom:7.1.0@pom
//DEPS com.fasterxml.jackson.core:jackson-annotations:2.21
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.1.0
//DEPS org.springframework.ai:spring-ai-client-chat:2.1.0

// Step 1: Spring AI Community agent-utils dependency provides the built-in SmartWebFetchTool.
//DEPS org.springaicommunity:spring-ai-agent-utils:0.14.0
//DEPS org.jsoup:jsoup:1.24.1


//DEPS software.amazon.awssdk:bedrockruntime:2.55.14
//DEPS software.amazon.awssdk:auth:2.55.14
//DEPS org.apache.httpcomponents.client5:httpclient5:5.6.5
//DEPS org.apache.httpcomponents.core5:httpcore5:5.4.4
//DEPS org.apache.httpcomponents.core5:httpcore5-h2:5.4.4
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//RUNTIME_OPTIONS -Daws.region=us-west-2 --enable-native-access=ALL-UNNAMED

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

// Step 2: Import the SmartWebFetchTool class from the community library.
import org.springaicommunity.agent.tools.SmartWebFetchTool;


private static final Logger log = LoggerFactory.getLogger("GameMasterWithBuiltInTools");

void main() {
    log.info("=== Starting Game Master AI Agent with Built-in Tools ===");

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

    var modelId = "us.anthropic.claude-haiku-5-5";
    var options = BedrockChatOptions.builder()
        .model(modelId)
        .build();

    var chatModel = BedrockProxyChatModel.builder()
        .bedrockRuntimeClient(bedrockClient)
        .options(options)
        .build();
    var agent = ChatClient.builder(chatModel).build();

    // The Game Master knows where the rules live: the Open5e API, an open Systems Reference Document.
    var systemPrompt = """
        You are a game master for a fantasy role-playing game.
        When asked about a spell or a monster, look it up on the Open5e API and answer from the data:
        - spells:   https://api.open5e.com/v2/spells/srd_<key>/
        - monsters: https://api.open5e.com/v2/creatures/srd_<key>/
        where <key> is the lowercase name with hyphens (e.g. fireball, adult-red-dragon).
        """;

    // Step 3: Create the SmartWebFetchTool — equips the agent to fetch and process web content
    var webFetchTool = SmartWebFetchTool.builder(agent)
        .maxContentLength(300_000)
        .build();

    try {
        // Step 4: Ask the agent and pass the web-fetch tool so it can read the Open5e API
        var response = agent.prompt()
            .system(systemPrompt)
            .user("What does the Fireball spell do, and how much damage does it deal?")
            .tools(webFetchTool)
            .call()
            .content();

        log.info("Agent Response:");
        log.info(response);
    } catch (Exception e) {
        log.error("Error invoking AI agent: {}", e.getMessage());
    } finally {
        log.info("\n=== Ending Game Master AI Agent with Built-in Tools ===");
    }
}
