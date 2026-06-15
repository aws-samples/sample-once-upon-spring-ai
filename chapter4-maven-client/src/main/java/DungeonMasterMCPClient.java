import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.bedrock.converse.BedrockProxyChatModel;
import org.springframework.ai.bedrock.converse.BedrockChatOptions;
import org.springframework.ai.chat.client.ChatClient;

// Provided for you: the four MCP client imports (the Spring AI bridge, the MCP client,
// the HTTP Streamable transport, and the MCP protocol schema types). JBang has no IDE
// to auto-resolve these package paths, so they're here ready to use.
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;

import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;

private static final Logger log = LoggerFactory.getLogger("DungeonMasterMCPClient");

void main() {
    // Step 0: Read the Bedrock API key from environment
    var bearerToken = System.getenv("AWS_BEARER_TOKEN_BEDROCK");
    if (bearerToken == null || bearerToken.isBlank()) {
        log.error("Set AWS_BEARER_TOKEN_BEDROCK first — get your key from the Amazon Bedrock Console → API keys → Short-term API keys");
        return;
    }

    // Step 1: Connect to the D&D Dice Roll MCP Server via Streamable HTTP
    log.info("Connecting to D&D Dice Roll MCP Server...");

    // TODO 1: Create the HTTP transport and MCP client
    //   Two steps:
    //   1. Build an HTTP Streamable transport pointing at localhost:8080 with the "/mcp" endpoint.
    //   2. Create a synchronous MCP client using that transport, with a client name and version.
    

    try {
        // TODO 2: Initialize the MCP client, discover tools, and bridge them to Spring AI.
        //   Three steps:
        //   1. Initialize the MCP client connection.
        //   2. List available tools from the server and log them.
        //   3. Build a SyncMcpToolCallbackProvider from the MCP client. It's a Spring AI
        //      ToolCallbackProvider — hand the provider itself to the agent via .tools(...) in
        //      TODO 3. (In Spring AI 2.0 the .tools(provider) path is preferred; manually pulling
        //      out a ToolCallback[] to pass around is the legacy approach.)

        // Step 4: Create AWS Bedrock ChatModel
        var bedrockClient = BedrockRuntimeClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(AnonymousCredentialsProvider.create())
                .overrideConfiguration(c -> c.putHeader("Authorization", "Bearer " + bearerToken))
                .build();

        var modelId = "us.anthropic.claude-haiku-4-5-20251001-v1:0";
        var options = BedrockChatOptions.builder()
                .model(modelId)
                .build();

        var chatModel = BedrockProxyChatModel.builder()
                .bedrockRuntimeClient(bedrockClient)
                .options(options)
                .build();

        // Step 5: Build ChatClient with system prompt (tools come from MCP Server remotely!)
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
                        // TODO 3: Pass the MCP tools to the agent so it can call the remote dice server.
                        
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
    }
}
