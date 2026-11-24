///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral
//DEPS io.netty:netty-bom:4.2.21.Final@pom
//DEPS tools.jackson:jackson-bom:3.1.8@pom
//DEPS org.springframework:spring-framework-bom:7.1.0@pom
//DEPS com.fasterxml.jackson.core:jackson-annotations:2.21
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.1.0
//DEPS org.springframework.ai:spring-ai-client-chat:2.1.0

// Step 1: The session advisors (they bring the Session API with them).
//DEPS org.springframework.ai:spring-ai-session-advisor:2.1.0

//DEPS software.amazon.awssdk:bedrockruntime:2.55.14
//DEPS software.amazon.awssdk:auth:2.55.14
//DEPS org.apache.httpcomponents.client5:httpclient5:5.6.5
//DEPS org.apache.httpcomponents.core5:httpcore5:5.4.4
//DEPS org.apache.httpcomponents.core5:httpcore5-h2:5.4.4
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17
//RUNTIME_OPTIONS -Daws.region=us-west-2 --enable-native-access=ALL-UNNAMED

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

    // Step 2: A session service keeps every turn in an append-only log. In-memory is enough for the workshop.
    var sessionService = DefaultSessionService.builder()
        .sessionRepository(InMemorySessionRepository.builder().build())
        .build();
    var sessionId = "chronicle";
    sessionService.findOrCreate(CreateSessionRequest.builder().id(sessionId).build());

    // Step 3: The session advisors remember the conversation. After 3 turns they compact the view
    //         to the last turn (tiny numbers so you see it happen), and SessionRecallTools lets the
    //         model search the log for what compaction dropped.
    var agent = ChatClient.builder(chatModel)
        .defaultSystem("""
            You are the Chronicler, the Game Master who records every deed of the adventurers.
            Answer in two or three sentences with theatrical flair.
            If the player asks about something you no longer see, search the conversation before answering.
            Never invent past events.
            """)
        .defaultAdvisors(SessionAdvisors.builder(sessionService)
            .compaction(new TurnCountTrigger(3), WindowCompactionStrategy.builder().lastTurns(1).build())
            .build())
        .defaultTools(SessionRecallTools.builder(sessionService).build())
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
            // Step 4: Every call names its session, and tells the recall tool which log to search.
            var response = agent.prompt()
                .user(userInput)
                .advisors(SessionAdvisors.session(sessionId))
                .toolContext(Map.of(SessionRecallTools.SESSION_ID_CONTEXT_KEY, sessionId))
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
