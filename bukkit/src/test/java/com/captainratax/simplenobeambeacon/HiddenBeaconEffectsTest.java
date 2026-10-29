package com.captainratax.simplenobeambeacon;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HiddenBeaconEffectsTest {
    @Test
    void tierFourUpgradesTheSelectedPrimaryEffect() throws Exception {
        List<PotionEffect> effects = apply(4, PotionEffectType.SPEED, null, true);

        assertEquals(1, effects.size());
        assertEquals(PotionEffectType.SPEED, effects.get(0).getType());
        assertEquals(1, effects.get(0).getAmplifier());
        assertEquals(340, effects.get(0).getDuration());
    }

    @Test
    void tierFourAppliesDistinctPrimaryAndSecondaryEffects() throws Exception {
        List<PotionEffect> effects = apply(4, PotionEffectType.SPEED, PotionEffectType.REGENERATION, false);

        assertEquals(2, effects.size());
        assertEquals(PotionEffectType.SPEED, effects.get(0).getType());
        assertEquals(0, effects.get(0).getAmplifier());
        assertEquals(PotionEffectType.REGENERATION, effects.get(1).getType());
        assertEquals(0, effects.get(1).getAmplifier());
        assertEquals(340, effects.get(1).getDuration());
    }

    @Test
    void lowerTiersDoNotApplyTierFourSelections() throws Exception {
        List<PotionEffect> effects = apply(3, PotionEffectType.SPEED, PotionEffectType.REGENERATION, true);

        assertEquals(1, effects.size());
        assertEquals(PotionEffectType.SPEED, effects.get(0).getType());
        assertEquals(0, effects.get(0).getAmplifier());
        assertEquals(300, effects.get(0).getDuration());
    }

    private static List<PotionEffect> apply(
            int tier,
            PotionEffectType primary,
            PotionEffectType secondary,
            boolean upgradedPrimary
    ) throws Exception {
        List<PotionEffect> effects = new ArrayList<>();
        Player player = (Player) Proxy.newProxyInstance(
                Player.class.getClassLoader(),
                new Class<?>[]{Player.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("addPotionEffect")) {
                        effects.add((PotionEffect) arguments[0]);
                        return true;
                    }
                    throw new AssertionError("Unexpected player call: " + method);
                }
        );
        HiddenBeacon beacon = new HiddenBeacon(
                UUID.randomUUID(), 0, 64, 0, tier, 50,
                primary, secondary, upgradedPrimary, new Object()
        );
        HiddenBeaconService service = new HiddenBeaconService(null, null);
        Method applyBeaconEffects = HiddenBeaconService.class.getDeclaredMethod(
                "applyBeaconEffects", Player.class, HiddenBeacon.class
        );
        applyBeaconEffects.setAccessible(true);
        applyBeaconEffects.invoke(service, player, beacon);
        return effects;
    }
}
