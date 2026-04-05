package com.amazonaws;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/// Immutable character data records — Java 25 style
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record Stats(int strength, int dexterity, int constitution, int intelligence, int wisdom, int charisma) {}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record InventoryItem(String itemName, int quantity) {}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record Character(
    String characterId, String name, String characterClass, String race, String gender,
    int level, int experience, Stats stats, List<InventoryItem> inventory, String createdAt
) {}

/// Persistent JSON-based character storage
@Service
class CharacterStore {

    private static final Logger log = LoggerFactory.getLogger(CharacterStore.class);
    private static final String DB_FILE = "characters.json";
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    List<Character> loadAll() {
        var file = new File(DB_FILE);
        if (!file.exists()) return new ArrayList<>();
        try {
            return mapper.readValue(file, new TypeReference<List<Character>>() {});
        } catch (IOException e) {
            log.error("Error reading characters DB: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    void saveAll(List<Character> characters) {
        try {
            mapper.writeValue(new File(DB_FILE), characters);
        } catch (IOException e) {
            log.error("Error writing characters DB: {}", e.getMessage());
        }
    }

    void insert(Character character) {
        var all = loadAll();
        all.add(character);
        saveAll(all);
    }
}

/// Character management tools — exposed to the A2A AgentExecutor via Spring AI @Tool.
/// Exposed to the A2A AgentExecutor via Spring AI @Tool annotations.
@Service
class CharacterTools {

    private static final Logger log = LoggerFactory.getLogger(CharacterTools.class);
    private final CharacterStore store;

    CharacterTools(CharacterStore store) {
        this.store = store;
    }

    @Tool(description = """
        Create a new D&D character. Roll dice to generate the stats_dict (ability scores).
        When rolling ability scores, remember the traditional method: roll 4d6, drop the lowest die.
        """)
    String createCharacter(
            @ToolParam(description = "Character's name") String name,
            @ToolParam(description = "D&D class (Fighter, Wizard, etc.)") String characterClass,
            @ToolParam(description = "D&D race (Human, Elf, etc.)") String race,
            @ToolParam(description = "Character's gender") String gender,
            @ToolParam(description = "Strength ability score") int strength,
            @ToolParam(description = "Dexterity ability score") int dexterity,
            @ToolParam(description = "Constitution ability score") int constitution,
            @ToolParam(description = "Intelligence ability score") int intelligence,
            @ToolParam(description = "Wisdom ability score") int wisdom,
            @ToolParam(description = "Charisma ability score") int charisma) {

        var characterId = UUID.randomUUID().toString();
        var stats = new Stats(strength, dexterity, constitution, intelligence, wisdom, charisma);
        var inventory = List.of(
                new InventoryItem("Starting Equipment Pack", 1),
                new InventoryItem("Gold Pieces", 100));

        var character = new Character(
                characterId, name, characterClass, race, gender,
                1, 0, stats, inventory, Instant.now().toString());

        store.insert(character);
        log.info("Created character: {} (ID: {}, {} {})", name, characterId, characterClass, race);

        return "Character created: %s — %s %s (ID: %s). Stats: STR=%d DEX=%d CON=%d INT=%d WIS=%d CHA=%d"
                .formatted(name, race, characterClass, characterId,
                        strength, dexterity, constitution, intelligence, wisdom, charisma);
    }

    @Tool(description = "Find a character by name")
    String findCharacterByName(@ToolParam(description = "The character's name to search for") String name) {
        log.info("Searching for character: '{}'", name);
        var match = store.loadAll().stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst();

        if (match.isEmpty()) return "Character with name '%s' not found".formatted(name);

        var c = match.get();
        return "Found: %s — %s %s, Level %d. Stats: STR=%d DEX=%d CON=%d INT=%d WIS=%d CHA=%d"
                .formatted(c.name(), c.race(), c.characterClass(), c.level(),
                        c.stats().strength(), c.stats().dexterity(), c.stats().constitution(),
                        c.stats().intelligence(), c.stats().wisdom(), c.stats().charisma());
    }

    @Tool(description = "List all characters in the database")
    String listAllCharacters() {
        var all = store.loadAll();
        if (all.isEmpty()) return "No characters found in the database.";

        var sb = new StringBuilder("Characters in database (%d):\n".formatted(all.size()));
        for (var c : all) {
            sb.append("- %s (%s %s, Level %d)\n".formatted(c.name(), c.race(), c.characterClass(), c.level()));
        }
        return sb.toString();
    }
}
