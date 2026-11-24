package com.amazonaws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

// Provided for you: the MCP annotation imports that expose your tools over the network.
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;

import java.util.Arrays;
import java.util.Random;

/// MCP Server that exposes dice rolling tools over HTTP.
/// Run this first, then run GameMasterMCPClient.java to connect.
/// The key difference from Chapter 3: tools are now accessible over the NETWORK via MCP protocol.
@SpringBootApplication
public class DiceRollMcpServer {

    public static void main(final String[] args) {
        SpringApplication.run(DiceRollMcpServer.class, args);
    }
}

@Component
class DiceTools {

    private static final Logger log = LoggerFactory.getLogger("DiceTools");
    private static final Random random = new Random();

    record DiceRollResponse(int[] rolls, int total, String description) {}

    // TODO 1: Annotate the method with @McpTool to expose it as an MCP tool

    // TODO 2: Annotate each parameter with @McpToolParam
    DiceRollResponse rollDice(int faces, int count) {

        var rolls = new int[count];
        var total = 0;

        for (int i = 0; i < count; i++) {
            rolls[i] = random.nextInt(faces) + 1;
            total += rolls[i];
        }

        var description = "Rolled %dd%d: %s = %d".formatted(count, faces, Arrays.toString(rolls), total);

        log.info("TOOL CALLED: {}", description);

        return new DiceRollResponse(rolls, total, description);
    }
}
