///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//FILES application.properties
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.boot:spring-boot-starter-web:4.0.2
//DEPS org.springframework.ai:spring-ai-starter-mcp-server-webmvc:2.0.0

package com.amazonaws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

// Step 1: MCP annotations expose Java methods as tools accessible over the wire.
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;


import java.util.Arrays;
import java.util.Random;

/// MCP Server that exposes dice rolling tools over HTTP.
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

    record DiceRollResponse(int[] rolls, int total, String description) {}

    // Step 2: @McpTool exposes this method as a tool clients can discover and invoke.
    // Step 3: @McpToolParam describes each parameter so the calling AI knows what to pass.
    @McpTool(description = "Roll dice for D&D game mechanics. Use this for attack rolls, damage, ability checks, or saving throws.")
    DiceRollResponse rollDice(
        @McpToolParam(description = "Number of faces on the dice (e.g. 6, 20)", required = true) int faces,
        @McpToolParam(description = "Number of dice to roll (e.g. 1, 3)", required = true) int count) {

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
