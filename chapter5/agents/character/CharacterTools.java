package com.amazonaws;

import com.fasterxml.jackson.annotation.JsonAlias;
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
import java.util.Optional;
import java.util.UUID;

/// Immutable character data records — Java 25 style
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record Stats(int strength, int dexterity, int constitution, int intelligence, int wisdom, int charisma) {}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record InventoryItem(@JsonAlias("itemName") String itemName, int quantity) {}

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
record Character(
    @JsonAlias("characterId") String characterId,
    String name,
    @JsonAlias("characterClass") String characterClass,
    String race, String gender,
    int level, int experience, Stats stats, List<InventoryItem> inventory,
    @JsonAlias("createdAt") String createdAt
) {}

/// Persistent JSON-based character storage — thread-safe via synchronized in-memory list.
@Service
class CharacterStore {

    private static final Logger log = LoggerFactory.getLogger(CharacterStore.class);
    private static final String DB_FILE = "characters.json";
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final List<Character> characters;

    CharacterStore() {
        var file = new File(DB_FILE);
        if (file.exists()) {
            try {
                characters = new ArrayList<>(mapper.readValue(file, new TypeReference<List<Character>>() {}));
            } catch (IOException e) {
                throw new RuntimeException("Failed to load characters DB", e);
            }
        } else {
            characters = new ArrayList<>();
        }
    }

    synchronized List<Character> getAll() {
        return List.copyOf(characters);
    }

    synchronized Optional<Character> findByName(String name) {
        return characters.stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst();
    }

    synchronized void insert(Character character) {
        characters.add(character);
        persist();
    }

    synchronized Optional<Character> updateByName(String name, java.util.function.UnaryOperator<Character> updater) {
        for (int i = 0; i < characters.size(); i++) {
            if (characters.get(i).name().equalsIgnoreCase(name)) {
                var updated = updater.apply(characters.get(i));
                characters.set(i, updated);
                persist();
                return Optional.of(updated);
            }
        }
        return Optional.empty();
    }

    private void persist() {
        try {
            mapper.writeValue(new File(DB_FILE), characters);
        } catch (IOException e) {
            log.error("Error writing characters DB: {}", e.getMessage());
        }
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

        return ("Character created: %s — %s %s (ID: %s). Stats: STR=%d DEX=%d CON=%d INT=%d WIS=%d CHA=%d. "
                + "Inventory: %s")
                .formatted(name, race, characterClass, characterId,
                        strength, dexterity, constitution, intelligence, wisdom, charisma,
                        formatInventory(inventory));
    }

    @Tool(description = "Find a character by name")
    String findCharacterByName(@ToolParam(description = "The character's name to search for") String name) {
        log.info("Searching for character: '{}'", name);
        var match = store.findByName(name);

        if (match.isEmpty()) return "Character with name '%s' not found".formatted(name);

        var c = match.get();
        return ("Found: %s — %s %s, Level %d, XP=%d. Stats: STR=%d DEX=%d CON=%d INT=%d WIS=%d CHA=%d. "
                + "Inventory: %s")
                .formatted(c.name(), c.race(), c.characterClass(), c.level(), c.experience(),
                        c.stats().strength(), c.stats().dexterity(), c.stats().constitution(),
                        c.stats().intelligence(), c.stats().wisdom(), c.stats().charisma(),
                        formatInventory(c.inventory()));
    }

    @Tool(description = "List all characters in the database")
    String listAllCharacters() {
        var all = store.getAll();
        if (all.isEmpty()) return "No characters found in the database.";

        var sb = new StringBuilder("Characters in database (%d):\n".formatted(all.size()));
        for (var c : all) {
            sb.append("- %s (%s %s, Level %d, Inventory: %s)\n"
                    .formatted(c.name(), c.race(), c.characterClass(), c.level(),
                            formatInventory(c.inventory())));
        }
        return sb.toString();
    }

    @Tool(description = "Add an item to a character's inventory. Use after looting, purchasing, or receiving quest rewards.")
    String addInventoryItem(
            @ToolParam(description = "The character's name") String characterName,
            @ToolParam(description = "Name of the item to add") String itemName,
            @ToolParam(description = "Quantity to add") int quantity) {

        log.info("Adding {}x {} to {}'s inventory", quantity, itemName, characterName);
        var result = store.updateByName(characterName, c -> {
            var updatedInventory = new ArrayList<>(c.inventory());
            var existing = updatedInventory.stream()
                    .filter(item -> item.itemName().equalsIgnoreCase(itemName))
                    .findFirst();

            if (existing.isPresent()) {
                var old = existing.get();
                updatedInventory.remove(old);
                updatedInventory.add(new InventoryItem(old.itemName(), old.quantity() + quantity));
            } else {
                updatedInventory.add(new InventoryItem(itemName, quantity));
            }

            return new Character(c.characterId(), c.name(), c.characterClass(), c.race(),
                    c.gender(), c.level(), c.experience(), c.stats(), updatedInventory, c.createdAt());
        });

        return result
                .map(c -> "Added %dx %s to %s's inventory. Current inventory: %s"
                        .formatted(quantity, itemName, c.name(), formatInventory(c.inventory())))
                .orElse("Character '%s' not found.".formatted(characterName));
    }

    @Tool(description = "Remove an item from a character's inventory. Use when items are consumed, sold, lost, or broken.")
    String removeInventoryItem(
            @ToolParam(description = "The character's name") String characterName,
            @ToolParam(description = "Name of the item to remove") String itemName,
            @ToolParam(description = "Quantity to remove") int quantity) {

        log.info("Removing {}x {} from {}'s inventory", quantity, itemName, characterName);

        var character = store.findByName(characterName);
        if (character.isEmpty()) return "Character '%s' not found.".formatted(characterName);

        var hasItem = character.get().inventory().stream()
                .anyMatch(item -> item.itemName().equalsIgnoreCase(itemName));
        if (!hasItem) return "%s doesn't have '%s' in their inventory.".formatted(character.get().name(), itemName);

        var result = store.updateByName(characterName, c -> {
            var updatedInventory = new ArrayList<>(c.inventory());
            var existing = updatedInventory.stream()
                    .filter(item -> item.itemName().equalsIgnoreCase(itemName))
                    .findFirst()
                    .get();

            updatedInventory.remove(existing);
            var remaining = existing.quantity() - quantity;
            if (remaining > 0) {
                updatedInventory.add(new InventoryItem(existing.itemName(), remaining));
            }

            return new Character(c.characterId(), c.name(), c.characterClass(), c.race(),
                    c.gender(), c.level(), c.experience(), c.stats(), updatedInventory, c.createdAt());
        });

        return result
                .map(c -> "Removed %dx %s from %s's inventory. Current inventory: %s"
                        .formatted(quantity, itemName, c.name(), formatInventory(c.inventory())))
                .orElse("Character '%s' not found.".formatted(characterName));
    }

    private static String formatInventory(List<InventoryItem> inventory) {
        if (inventory == null || inventory.isEmpty()) return "empty";
        return inventory.stream()
                .map(item -> "%s x%d".formatted(item.itemName(), item.quantity()))
                .collect(java.util.stream.Collectors.joining(", "));
    }
}
