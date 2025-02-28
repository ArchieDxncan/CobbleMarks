package com.cobblemon.fabric.example;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.fabricmc.api.ModInitializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.IntTag;

public class MarkServer implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("CobbleMarks");

    // Lists of mark IDs based on their categories
    private static final int RARE_MARK_ID = 69; // 1 in 1000 chance
    private static final int UNCOMMON_MARK_ID = 68; // 1 in 50 chance
    private static final List<Integer> VERY_RARE_MARKS = Arrays.asList(
            70, 71, 72, 73, 74, 75, 76, 77, 78, 79, 80, 81, 82,
            83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97

    ); // 1 in 2800 chance

    private static final Random RANDOM = new Random();

    @Override
    public void onInitialize() {
        // Subscribe to Cobblemon events for server-side handling
        CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.NORMAL, event -> {
            try {
                handlePokemonEvent(event.getPokemon(), event.getPlayer());
            } catch (Exception e) {
                logError("POKEMON_CAPTURED", e);
            }
            return null;
        });

        LOGGER.info("Server-side initialization complete.");
    }

    private void handlePokemonEvent(Pokemon pokemon, ServerPlayer player) {
        try {
            // Check for time-based marks (1 in 50 chance)
            if (RANDOM.nextInt(50) == 0) {
                int markId = getTimeBasedMark((ServerLevel) player.level());
                if (markId != -1) { // -1 means no valid weather condition
                    addRibbon(pokemon, markId, player);
                    LOGGER.info("Assigned time-based mark ID {} to Pokémon {}", markId, pokemon.getSpecies().getName());
                    return; // Exit after assigning a mark
                }
            }

            // Check for weather-based marks (1 in 50 chance)
            if (RANDOM.nextInt(50) == 0) {
                int markId = getWeatherBasedMark((ServerLevel) player.level(), player);
                if (markId != -1) { // -1 means no valid weather condition
                    addRibbon(pokemon, markId, player);
                    LOGGER.info("Assigned weather-based mark ID {} to Pokémon {}", markId, pokemon.getDisplayName().getString());
                    return; // Exit after assigning a mark
                }
            }

            // Check for rare mark (1 in 1000 chance)
            if (RANDOM.nextInt(1000) == 0) {
                addRibbon(pokemon, RARE_MARK_ID, player);
                LOGGER.info("Assigned rare mark ID {} to Pokémon {}", RARE_MARK_ID, pokemon.getSpecies().getName());
                return; // Exit after assigning a mark
            }

            // Check for uncommon mark (1 in 50 chance)
            if (RANDOM.nextInt(50) == 0) {
                addRibbon(pokemon, UNCOMMON_MARK_ID, player);
                LOGGER.info("Assigned uncommon mark ID {} to Pokémon {}", UNCOMMON_MARK_ID, pokemon.getSpecies().getName());
                return; // Exit after assigning a mark
            }

            // Check for very rare marks (1 in 2800 chance)
            if (RANDOM.nextInt(2800) == 0) {
                int markId = VERY_RARE_MARKS.get(RANDOM.nextInt(VERY_RARE_MARKS.size()));
                addRibbon(pokemon, markId, player);
                LOGGER.info("Assigned very rare mark ID {} to Pokémon {}", markId, pokemon.getSpecies().getName());
                return; // Exit after assigning a mark
            }
        } catch (Exception e) {
            logError("PokemonEvent", e);
        }
    }

    private int getTimeBasedMark(ServerLevel level) {
        long time = level.getDayTime() % 24000; // Get the current time in ticks (0-23999)

        // Corrected time ranges
        if (time >= 6000 && time < 7000) {
            // Midday (12:00 PM - 1:00 PM)
            return 53; // Lunchtime Mark
        } else if (time >= 13000 && time < 23000) {
            // Night (7:00 PM - 5:00 AM)
            return 54; // Sleepy-Time Mark
        } else if (time >= 12000 && time < 13000) {
            // Dusk (6:00 PM - 7:00 PM)
            return 55; // Dusk Mark
        } else if (time >= 0 && time < 1000) {
            // Dawn (6:00 AM - 7:00 AM)
            return 56; // Dawn Mark
        }

        return -1; // No valid time-based mark
    }

    // Helper method to get a weather-based mark based on the current weather in the world
    private int getWeatherBasedMark(ServerLevel level, ServerPlayer player) {

        // Check if it's snowing (in cold biomes)
        if (level.isRaining() && level.getBiome(player.getOnPos()).value().coldEnoughToSnow(player.getOnPos())) {
            return 60; // Snowy Mark

        }// Check if it's raining
        if (level.isRaining()) {
            // Check if it's thundering (stormy weather)
            if (level.isThundering()) {
                return 59; // Stormy Mark
            } else {
                return 58; // Rainy Mark
            }
        }

        // Check for other weather conditions (e.g., sandstorm, fog)
        // Note: Minecraft doesn't have built-in sandstorm or fog, so these would require custom logic.
        // For now, we'll assume clear weather or other conditions are not applicable for marks.
        return -1; // No valid weather-based mark
    }

    // Utility method to add a mark to the "Ribbons" list
    private void addRibbon(Pokemon pokemon, int markId, ServerPlayer player) {
        CompoundTag data = pokemon.getPersistentData();

        // Check if the "Ribbons" tag already exists and is an int array (type 11 = IntArrayTag)
        int[] existingMarks = new int[0];
        if (data.contains("Ribbons", 11)) { // 11 = IntArrayTag
            existingMarks = data.getIntArray("Ribbons");
        }

        // Check if mark already exists (to prevent duplicates)
        for (int mark : existingMarks) {
            if (mark == markId) {
                return; // Already present, skip
            }
        }

        // Add new mark to array
        int[] updatedMarks = new int[existingMarks.length + 1];
        System.arraycopy(existingMarks, 0, updatedMarks, 0, existingMarks.length);
        updatedMarks[existingMarks.length] = markId;

        // Save updated array back into persistent data
        data.putIntArray("Ribbons", updatedMarks);
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal("This Pokémon seems to have a special mark?"));

    }

    private void logError(String eventName, Exception e) {
        // Generalized error logging for event handlers
        LOGGER.error("An error occurred in the " + eventName + " event handler:", e);
    }
}