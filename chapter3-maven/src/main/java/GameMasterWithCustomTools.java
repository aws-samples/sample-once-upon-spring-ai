///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//SOURCES DiceTools.java
//REPOS mavencentral
//DEPS io.netty:netty-bom:4.2.21.Final@pom
//DEPS tools.jackson:jackson-bom:3.1.8@pom
//DEPS org.springframework:spring-framework-bom:7.1.0@pom
//DEPS com.fasterxml.jackson.core:jackson-annotations:2.21
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.1.0
//DEPS org.springframework.ai:spring-ai-client-chat:2.1.0
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
import java.util.List;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallDecision;
import org.springframework.ai.model.tool.ToolCallDecisions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

private static final Logger log = LoggerFactory.getLogger("GameMasterWithCustomTools");

void main() {
    log.info("=== Starting Game Master AI Agent with Custom Tools ===");

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

    var agent = ChatClient.builder(chatModel)
        .defaultSystem("""
            You are Lady Luck, the mystical keeper of dice and fortune in TTRPG adventures.
            You speak with theatrical flair and always announce dice rolls with appropriate drama.
            You know all about TTRPG mechanics, ability scores, and can help players with character creation.
            When rolling ability scores, remember the traditional method: roll 4d6, drop the lowest die.
            """)
        .build();

    var playerMessage = "Help me create a new TTRPG character! Roll the strength, wisdom, charisma and intelligence abilities scores using 4d6 drop lowest method.";
    log.info("Player: {}\n", playerMessage);

    var diceTools = new DiceTools();
    // Same default manager the ChatClient uses: it honors requiresConfirmation from the @Tool annotation.
    var toolCallingManager = ToolCallingManager.builder().build();
    List<Message> conversation = List.of(new UserMessage(playerMessage));

    try {
        // Step 4: Equip the agent with the DiceTools so it can roll dice during reasoning
        ChatResponse reply = agent.prompt()
            .messages(conversation)
            .tools(diceTools)
            .call()
            .chatResponse();

        // Step 6: The loop paused before rolling. Ask the player, run the allowed rolls, then continue.
        while (ToolCallDecisions.isAwaitingDecision(reply)) {
            var toolCalls = reply.getResult().getOutput().getToolCalls();
            toolCalls.forEach(call -> IO.println("Lady Luck wants to call %s %s".formatted(call.name(), call.arguments())));
            var allowed = IO.readln("Let the dice roll? (y/n) ").trim().equalsIgnoreCase("y");

            var decisions = ToolCallDecisions.toolCallKeys(reply.getResult().getOutput()).stream()
                .map(key -> allowed
                    ? ToolCallDecision.allow(key)
                    : ToolCallDecision.deny(key, "The player refused to roll. Ask what they want instead."))
                .toArray(ToolCallDecision[]::new);

            var toolOptions = ToolCallingChatOptions.builder().toolCallbacks(ToolCallbacks.from(diceTools)).build();
            var result = toolCallingManager.executeToolCalls(
                new Prompt(conversation, toolOptions), reply, ToolCallDecisions.of(decisions));
            conversation = result.conversationHistory();

            reply = agent.prompt()
                .messages(conversation)
                .tools(diceTools)
                .call()
                .chatResponse();
        }

        log.info("Game Master says:");
        log.info(reply.getResult().getOutput().getText());
    } catch (Exception e) {
        log.error("Error invoking AI agent: {}", e.getMessage());
    } finally {
        log.info("\n=== Ending Game Master AI Agent with Custom Tools ===");
    }
}
