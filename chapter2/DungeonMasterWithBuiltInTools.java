///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//SOURCES ../config/BedrockChatModelConfig.java
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M4

//DEPS org.springaicommunity:spring-ai-agent-utils:0.5.0

//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import org.springaicommunity.agent.tools.SmartWebFetchTool;

private static final Logger log = LoggerFactory.getLogger("DungeonMasterWithBuiltInTools");

void main() {
    log.info("=== Starting Dungeon Master AI Agent with Built-in Tools ===");

    var chatModel = BedrockChatModelConfig.createChatModel();

    var agent = ChatClient.builder(chatModel).build();
    
    // Step 4: Create SmartWebFetchTool tool to enable the AI agent to fetch / process web content
    var webFetch = SmartWebFetchTool.builder(agent).maxContentLength(300_000).build();

    try {
        // Step 5: Ask the AI to fetch and extract information from Wikipedia
        var response = agent.prompt()
            .user("Using the website https://en.wikipedia.org/wiki/Dungeons_%26_Dragons tell me the name of the designers of Dungeons and Dragons.")
            .call()
            .content();

        log.info("Agent Response:");
        log.info(response);
    } catch (Exception e) {
        log.error("Error invoking AI agent: {}", e.getMessage());
    } finally {
        log.info("\n=== Ending Dungeon Master AI Agent with Built-in Tools ===");
    }
}
