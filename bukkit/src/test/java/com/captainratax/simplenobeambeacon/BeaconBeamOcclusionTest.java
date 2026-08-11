package com.captainratax.simplenobeambeacon;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BeaconBeamOcclusionTest {
    @Test
    void exactRuleLetsBarrierThrough() {
        assertFalse(ServerCompatibility.blocksFromVanillaValues(Material.BARRIER, 0, false));
        assertFalse(ServerCompatibility.blocksFromVanillaValues(Material.BARRIER, 1, false));
    }

    @Test
    void exactRulePreservesVanillaExceptions() {
        assertFalse(ServerCompatibility.blocksFromVanillaValues(Material.BEDROCK, 15, false));
        assertFalse(ServerCompatibility.blocksFromVanillaValues(Material.GLASS, 0, false));
        assertFalse(ServerCompatibility.blocksFromVanillaValues(Material.RED_STAINED_GLASS, 15, true));
        assertFalse(ServerCompatibility.blocksFromVanillaValues(Material.BLUE_STAINED_GLASS_PANE, 0, true));
        assertFalse(ServerCompatibility.blocksFromVanillaValues(Material.WATER, 1, false));
        assertFalse(ServerCompatibility.blocksFromVanillaValues(Material.OAK_LEAVES, 1, false));
    }

    @Test
    void tintedGlassIsThePluginPassThroughException() {
        assertFalse(ServerCompatibility.blocksFromVanillaValues(Material.TINTED_GLASS, 15, false));
    }

    @Test
    void exactRuleStillRejectsOpaqueBlocks() {
        assertTrue(ServerCompatibility.blocksFromVanillaValues(Material.STONE, 15, false));
        assertTrue(ServerCompatibility.blocksFromVanillaValues(Material.OBSIDIAN, 15, false));
    }

    @Test
    void compatibilityFallbackCorrectsTheKnownMaterialMismatches() {
        assertFalse(ServerCompatibility.blocksFromBukkitFallback(Material.BARRIER, true));
        assertFalse(ServerCompatibility.blocksFromBukkitFallback(Material.BEDROCK, true));
        assertFalse(ServerCompatibility.blocksFromBukkitFallback(Material.SLIME_BLOCK, true));
        assertTrue(ServerCompatibility.blocksFromBukkitFallback(Material.PACKED_ICE, true));
        assertTrue(ServerCompatibility.blocksFromBukkitFallback(Material.BLUE_ICE, true));
        assertFalse(ServerCompatibility.blocksFromBukkitFallback(Material.TINTED_GLASS, true));
        assertFalse(ServerCompatibility.blocksFromBukkitFallback(Material.GLASS, false));
        assertTrue(ServerCompatibility.blocksFromBukkitFallback(Material.STONE, true));
    }
}
