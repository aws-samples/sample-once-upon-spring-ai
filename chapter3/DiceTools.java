///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO 1: Import the @Tool and @ToolParam annotations from Spring AI.
//   These annotations tell the AI model what your method does and what each parameter means.

import java.util.Arrays;
import java.util.Random;

/// Tool class containing D&D dice rolling methods the AI can call
/// Demonstrates Java 25 records and modern features
class DiceTools {

    private static final Logger log = LoggerFactory.getLogger("DiceTools");
    private static final Random random = new Random();

    /// Record for dice roll output - Java 25 immutable data carrier
    record DiceRollResponse(int[] rolls, int total, String description) {}

    // TODO 2: Add the @Tool annotation with a description that tells the AI when to use this method.
    //   The description is critical — it's how the AI decides whether to call your tool.
    //   Example: @Tool(description = "Roll dice for D&D game mechanics. Use this for attack rolls, damage, ability checks, or saving throws.")

    // TODO 3: Add @ToolParam annotations to each parameter so the AI knows what values to pass.
    //   Example: @ToolParam(description = "Number of faces on the dice (e.g. 6, 20)", required = true) int faces,
    //            @ToolParam(description = "Number of dice to roll (e.g. 1, 3)", required = true) int count
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
