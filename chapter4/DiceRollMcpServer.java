///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+
//REPOS mavencentral,spring-milestones=https://repo.spring.io/milestone
//DEPS org.springframework.boot:spring-boot-starter-web:4.0.2
//DEPS org.springframework.ai:spring-ai-starter-mcp-server-webflux:2.0.0-M2
//DEPS org.springaicommunity:mcp-annotations:0.8.0
//DEPS io.modelcontextprotocol.sdk:mcp:1.0.0

package com.amazonaws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;

import java.util.Arrays;
import java.util.Random;

/// MCP Server that exposes D&D dice rolling tools over HTTP
/// Run this first, then run DungeonMasterMCPClient.java to connect
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

    @McpTool(description = "Roll dice for D&D game mechanics. Use this for attack rolls, damage, ability checks, or saving throws.")
    DiceRollResponse rollDice(
        @McpToolParam(description = "Number of faces on the dice (e.g. 6, 20)", required = true) int faces,
        @McpToolParam(description = "Number of dice to roll (e.g. 1, 3)", required = true) int count) 
    {
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

// /// Exposes D&D prompt templates via the MCP Prompts capability
// @Configuration
// class DicePrompts {

//     @Bean
//     List<McpServerFeatures.SyncPromptSpecification> dmPrompts() {

//         // Prompt 1: Create a D&D character with rolled ability scores
//         var createCharacter = new McpSchema.Prompt(
//             "create_character",
//             "Generate D&D character ability scores by rolling 4d6 drop lowest for each stat",
//             List.of(
//                 new McpSchema.PromptArgument("name", "Character name", true),
//                 new McpSchema.PromptArgument("character_class", "D&D class (e.g. Fighter, Wizard, Rogue)", true)
//             )
//         );

//         var createCharacterSpec = new McpServerFeatures.SyncPromptSpecification(
//             createCharacter,
//             (exchange, request) -> {
//                 var name = request.arguments().get("name");
//                 var characterClass = request.arguments().get("character_class");

//                 var promptText = """
//                     Create a D&D character named %s who is a %s.

//                     Roll ability scores using the standard method:
//                     - Roll 4d6 (use rollDice with faces=6, count=4) for each of the 6 abilities
//                     - Drop the lowest die from each roll
//                     - The abilities are: Strength, Dexterity, Constitution, Intelligence, Wisdom, Charisma

//                     After rolling, assign the scores to abilities in a way that makes sense \
//                     for a %s, and provide a brief character backstory.
//                     """.formatted(name, characterClass, characterClass);

//                 return new GetPromptResult(
//                     "Create D&D Character: " + name,
//                     List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
//                 );
//             }
//         );

//         // Prompt 2: Narrate a combat encounter with dice rolls
//         var narrateCombat = new McpSchema.Prompt(
//             "narrate_combat",
//             "Set up and narrate a D&D combat encounter with dice rolls",
//             List.of(
//                 new McpSchema.PromptArgument("attacker", "Name of the attacking character", true),
//                 new McpSchema.PromptArgument("defender", "Name of the defending creature", true),
//                 new McpSchema.PromptArgument("weapon", "Weapon used (e.g. longsword, shortbow)", false)
//             )
//         );

//         var narrateCombatSpec = new McpServerFeatures.SyncPromptSpecification(
//             narrateCombat,
//             (exchange, request) -> {
//                 var attacker = request.arguments().get("attacker");
//                 var defender = request.arguments().get("defender");
//                 var weapon = request.arguments().getOrDefault("weapon", "their weapon");

//                 var promptText = """
//                     Narrate a D&D combat round where %s attacks %s with %s.

//                     Follow these steps:
//                     1. Roll a d20 for the attack (use rollDice with faces=20, count=1)
//                     2. If the roll is 10 or higher, the attack hits — roll damage
//                     3. For damage, roll appropriate dice for the weapon \
//                     (e.g. 1d8 for longsword, 1d6 for shortbow)
//                     4. A natural 20 is a critical hit — roll damage dice twice!

//                     Narrate the action with dramatic flair.
//                     """.formatted(attacker, defender, weapon);

//                 return new GetPromptResult(
//                     "Combat: " + attacker + " vs " + defender,
//                     List.of(new PromptMessage(Role.USER, new TextContent(promptText)))
//                 );
//             }
//         );

//         return List.of(createCharacterSpec, narrateCombatSpec);
//     }
// }