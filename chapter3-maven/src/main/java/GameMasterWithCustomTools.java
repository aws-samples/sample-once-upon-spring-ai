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
        ChatResponse reply = agent.prompt()
            .messages(conversation)
            // TODO 4: Pass the DiceTools to the agent using .tools()
            .call()
            .chatResponse();

        // Provided for you: once TODO 5 marks the tool with requiresConfirmation, the loop pauses before
        // rolling. This block asks the player, runs the allowed rolls, then lets the agent continue.
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
