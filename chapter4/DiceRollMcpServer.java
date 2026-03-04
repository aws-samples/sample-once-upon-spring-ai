///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.boot:spring-boot-starter-web:4.0.2
//DEPS org.springframework.ai:spring-ai-starter-mcp-server-webmvc:2.0.0-M2
//DEPS org.springaicommunity:mcp-annotations:0.8.0
//DEPS io.modelcontextprotocol.sdk:mcp:1.0.0

package com.amazonaws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

// TODO 1: Import the MCP annotation classes that will expose your tools over the network.
//   These are the MCP equivalents of @Tool and @ToolParam from Chapter 3.

import java.util.Arrays;
import java.util.Random;

/// MCP Server that exposes D&D dice rolling tools over HTTP.
/// Run this first, then run DungeonMasterMCPClient.java to connect.
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

    /// Record for dice roll output - Java 25 immutable data carrier
    record DiceRollResponse(int[] rolls, int total, String description) {}

    // TODO 2: Annotate this method with @McpTool to expose it as a network-accessible MCP tool.
    //   This is similar to @Tool from Chapter 3, but the tool will be available over HTTP.

    // TODO 3: Annotate each parameter with @McpToolParam to describe them for MCP clients.
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
