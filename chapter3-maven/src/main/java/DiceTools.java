///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25+

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO 1: Import @Tool and @ToolParam annotations from Spring AI


import java.util.Arrays;
import java.util.Random;

/// Tool class containing D&D dice rolling methods the AI can call
class DiceTools {

    private static final Logger log = LoggerFactory.getLogger("DiceTools");
    private static final Random random = new Random();

    record DiceRollResponse(int[] rolls, int total, String description) {}

    // TODO 2: Add the @Tool annotation with a description telling the AI when to use this method

    // TODO 3: Add @ToolParam annotations to each parameter so the AI knows what values to pass
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
