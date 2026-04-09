///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.ai:spring-ai-bedrock-converse:2.0.0-M4
//DEPS org.springframework.ai:spring-ai-client-chat:2.0.0-M4
//DEPS org.springframework.ai:spring-ai-mcp:2.0.0-M4
//DEPS io.modelcontextprotocol.sdk:mcp:1.0.0
//DEPS software.amazon.awssdk:bedrockruntime:2.41.34
//DEPS software.amazon.awssdk:auth:2.41.34
//DEPS org.slf4j:slf4j-api:2.0.17
//DEPS org.slf4j:slf4j-simple:2.0.17

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

private static final Logger log = LoggerFactory.getLogger("DungeonMasterMCPClient");

void main() {
    // Step 1: Connect to the D&D Dice Roll MCP Server via Streamable HTTP
    log.info("Connecting to D&D Dice Roll MCP Server...");

    var transport = HttpClientStreamableHttpTransport.builder("http://localhost:8080")
            .endpoint("/mcp")
            .build();

    var mcpClient = McpClient.sync(transport)
            .clientInfo(new McpSchema.Implementation("dice-mcp-client", "1.0.0"))
            .build();

    try {
        mcpClient.initialize();

        // Step 2: Discover available tools from the MCP Server
        var toolsResult = mcpClient.listTools();
        var toolNames = toolsResult.tools().stream().map(McpSchema.Tool::name).toList();
        log.info("Available tools: {}", toolNames);

        // Step 3: Bridge MCP tools into Spring AI ToolCallbacks
        var mcpToolProvider = SyncMcpToolCallbackProvider.builder()
                .mcpClients(mcpClient)
                .build();
        var mcpTools = mcpToolProvider.getToolCallbacks();

        // Step 4: Create AWS Bedrock ChatModel
        var bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();

        var modelId = "global.anthropic.claude-haiku-4-5-20251001-v1:0";
        var options = BedrockChatOptions.builder()
                .model(modelId)
                .build();

        var chatModel = BedrockProxyChatModel.builder()
                .bedrockRuntimeClient(bedrockClient)
                .defaultOptions(options)
                .build();

        // Step 5: Build ChatClient with system prompt (tools registered from MCP Server)
        var agent = ChatClient.builder(chatModel)
                .defaultSystem("""
                        You are Lady Luck, the mystical keeper of dice and fortune in D&D adventures.
                        You speak with theatrical flair and always announce dice rolls with appropriate drama.
                        You know all about D&D mechanics, always use the appropriate tools when applicable - never make up results!
                        """)
                .build();

        // Step 6: Start interactive session - tools come from the remote MCP Server
        IO.println("\n\uD83C\uDFB2 Lady Luck - D&D Gamemaster with MCP Dice Rolling");
        IO.println("=".repeat(60));
        IO.println("\n\uD83C\uDFAF Try: 'Roll a d20' or 'Roll a d6' or 'Roll a d100'");

        var exitCommands = Set.of("exit", "quit", "bye");

        while (true) {
            var userInput = IO.readln("\n\uD83C\uDFB2 Your request: ").trim();

            if (exitCommands.contains(userInput.toLowerCase())) {
                IO.println("\uD83C\uDFAD May fortune favor your future adventures!");
                break;
            }

            IO.println("\n\uD83C\uDFB2 Rolling the dice of fate...\n");

            try {
                var response = agent.prompt()
                        .user(userInput)
                        .toolCallbacks(mcpTools)
                        .call()
                        .content();

                IO.println(response);
            } catch (Exception e) {
                log.error("Error invoking AI agent: {}", e.getMessage());
            }
        }

    } catch (Exception e) {
        IO.println("Connection failed: " + e.getMessage());
        IO.println("Make sure the dice service is running: jbang DiceRollMcpServer.java");
    } finally {
        mcpClient.closeGracefully();
    }
}
