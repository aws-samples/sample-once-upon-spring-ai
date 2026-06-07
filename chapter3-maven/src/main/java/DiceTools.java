import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Provided for you: the @Tool and @ToolParam imports (JBang has no IDE to auto-import them).
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Arrays;
import java.util.Random;

/// Tool class containing D&D dice rolling methods the AI can call
class DiceTools {

    private static final Logger log = LoggerFactory.getLogger("DiceTools");
    private static final Random random = new Random();

    record DiceRollResponse(int[] rolls, int total, String description) {}

    // TODO 1: Add the @Tool annotation with a description telling the AI when to use this method

    // TODO 2: Add @ToolParam annotations to each parameter so the AI knows what values to pass
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
