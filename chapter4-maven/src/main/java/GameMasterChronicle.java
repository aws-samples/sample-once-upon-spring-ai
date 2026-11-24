// Provided for you: the session advisors dependency (it brings the Session API with it).


import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.advisor.SessionAdvisors;
import org.springframework.ai.session.compaction.TurnCountTrigger;
import org.springframework.ai.session.compaction.WindowCompactionStrategy;
import org.springframework.ai.session.tool.SessionRecallTools;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

private static final Logger log = LoggerFactory.getLogger("GameMasterChronicle");

void main() {
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

    var chatModel = BedrockProxyChatModel.builder()
        .bedrockRuntimeClient(bedrockClient)
        .options(BedrockChatOptions.builder().model("us.anthropic.claude-haiku-5-5").build())
        .build();

    // Provided for you: a session service keeps every turn in an append-only log. In-memory is enough for the workshop.
    var sessionService = DefaultSessionService.builder()
        .sessionRepository(InMemorySessionRepository.builder().build())
        .build();
    var sessionId = "chronicle";
    sessionService.findOrCreate(CreateSessionRequest.builder().id(sessionId).build());

    var agent = ChatClient.builder(chatModel)
        .defaultSystem("""
            You are the Chronicler, the Game Master who records every deed of the adventurers.
            Answer in two or three sentences with theatrical flair.
            If the player asks about something you no longer see, search the conversation before answering.
            Never invent past events.
            """)
        // TODO 1: Add the session advisors so the agent remembers the conversation.
        //   Build them with SessionAdvisors.builder(sessionService) and add a compaction:
        //   after 3 turns (TurnCountTrigger), keep only the last turn (WindowCompactionStrategy).
        //   The numbers are tiny on purpose, so you see compaction happen in a few turns.

        // TODO 2: Register SessionRecallTools as a default tool so the model can search what compaction dropped.

        .build();

    IO.println("\n📜 The Chronicler - a Game Master who never forgets");
    IO.println("=".repeat(60));
    IO.println("Try: 'I hide the silver key under the third barrel', then three other actions,");
    IO.println("then 'Where did I hide the silver key?'");

    var exitCommands = Set.of("exit", "quit", "bye");
    while (true) {
        var userInput = IO.readln("\n🧙 Your deed: ").trim();
        if (exitCommands.contains(userInput.toLowerCase())) {
            IO.println("📜 Your deeds are written. Farewell, adventurer!");
            break;
        }
        if (userInput.isEmpty()) {
            continue;
        }

        try {
            var response = agent.prompt()
                .user(userInput)
                // TODO 3: Name the session on every call with .advisors(SessionAdvisors.session(sessionId)),
                //   and tell the recall tool which log to search with
                //   .toolContext(Map.of(SessionRecallTools.SESSION_ID_CONTEXT_KEY, sessionId)).

                .call()
                .content();
            IO.println(response);

            // Watch compaction: the model's view shrinks, the log keeps growing.
            IO.println("   [chronicle] messages the model sees: %d | events in the log: %d".formatted(
                sessionService.getActiveMessages(sessionId).size(),
                sessionService.getEvents(sessionId).size()));
        } catch (Exception e) {
            log.error("Error invoking AI agent: {}", e.getMessage());
        }
    }
}
