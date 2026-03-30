///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Arrays;
import java.util.Random;

/// Tool class containing D&D dice rolling methods the AI can call
/// Demonstrates Java 25 records and modern features
class DiceTools {

    private static final Logger log = LoggerFactory.getLogger("DiceTools");
    private static final Random random = new Random();

    record DiceRollResponse(int[] rolls, int total, String description) {}

    @Tool(description = "Roll dice for D&D game mechanics. Use this for attack rolls, damage, ability checks, or saving throws.")
    DiceRollResponse rollDice(
                @ToolParam(description = "Number of faces on the dice (e.g. 6, 20)", required = true) int faces,
                @ToolParam(description = "Number of dice to roll (e.g. 1, 3)", required = true) int count) 
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
