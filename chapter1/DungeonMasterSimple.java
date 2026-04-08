///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//SOURCES ../config/BedrockChatModelConfig.java
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M4
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

private static final Logger log = LoggerFactory.getLogger("DungeonMasterSimple");

void main() {

    log.info("=== Starting Dungeon Master AI Agent ===");

    var chatModel = BedrockChatModelConfig.createChatModel();

    // Step 5: Build ChatClient with system prompt (defines AI personality)
    var agent = ChatClient.builder(chatModel)
        .defaultSystem("""
            You are a dungeon master for a D&D game.
            You describe the environment, creatures, and challenges.
            You respond in a terse and humorous way.
            """)
        .build();

    // Step 6: Invoke the AI agent
    var playerMessage = "Hi, I am an adventurer ready for adventure!";
    log.info("Player: " + playerMessage + "\n");

    try {
        var response = agent.prompt()
            .user(playerMessage)
            .call()
            .content();
        log.info(response);
    } catch (Exception e) {
        log.error("Error invoking AI agent: {}", e.getMessage());
    } finally {
        log.info("\n=== Ending Dungeon Master AI Agent ===");
    }
}
