package com.captainratax.simplenobeambeacon;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Beacon;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Keeps version-specific server access behind reflection so the plugin can
 * retain a Bukkit 1.17 binary baseline while using the exact Paper 26.2 data.
 */
final class ServerCompatibility {
    private static final String PAPER_EFFECT_RANGE_TAG = "Paper.Range";

    private final Plugin plugin;
    private volatile BeamAccess beamAccess;
    private volatile EffectAccess effectAccess;
    private volatile RangeAccess rangeAccess;
    private volatile boolean effectAccessUnavailable;
    private volatile boolean rangeAccessUnavailable;
    private volatile boolean reportedBeamFallback;
    private volatile boolean reportedEffectFallback;

    ServerCompatibility(Plugin plugin) {
        this.plugin = plugin;
    }

    boolean blocksBeaconBeam(Block block) {
        Material type = block.getType();
        if (type == Material.TINTED_GLASS || type == Material.BEDROCK) {
            return false;
        }

        BlockData data = block.getBlockData();
        BeamAccess access = beamAccess;
        if (access == null) {
            access = discoverBeamAccess(block, data);
            beamAccess = access;
        }

        if (access != BeamAccess.UNAVAILABLE) {
            try {
                Object state = access.getState.invoke(data);
                Object lightDampening;
                if (access.getWorldHandle == null) {
                    lightDampening = access.getLightDampening.invoke(state);
                } else {
                    Object worldHandle = access.getWorldHandle.invoke(block.getWorld());
                    Object position = access.getBlockPosition.invoke(block);
                    lightDampening = access.getLightDampening.invoke(
                            state,
                            worldHandle,
                            position
                    );
                }
                return blocksFromVanillaValues(type, ((Number) lightDampening).intValue(), false);
            } catch (IllegalAccessException | InvocationTargetException | ClassCastException exception) {
                beamAccess = BeamAccess.UNAVAILABLE;
                reportBeamFallback(exception);
            }
        }

        return blocksFromBukkitFallback(type, type.isOccluding());
    }

    SelectedEffects readSelectedEffects(Beacon beacon) {
        EffectAccess access = effectAccess;
        if (access == null && !effectAccessUnavailable) {
            access = discoverEffectAccess(beacon);
            if (access == null) {
                effectAccessUnavailable = true;
            } else {
                effectAccess = access;
            }
        }

        if (access != null) {
            try {
                Object handle = access.getBlockEntity.invoke(beacon);
                Object primaryValue = access.primaryPower.get(handle);
                if (primaryValue == null) {
                    return null;
                }

                Object secondaryValue = access.secondaryPower.get(handle);
                PotionEffectType primary = convertEffect(access, primaryValue);
                PotionEffectType secondary = secondaryValue == null
                        ? null
                        : convertEffect(access, secondaryValue);
                if (primary != null && (secondaryValue == null || secondary != null)) {
                    return new SelectedEffects(
                            primary,
                            secondary,
                            primaryValue.equals(secondaryValue),
                            true
                    );
                }
            } catch (ReflectiveOperationException | ClassCastException exception) {
                effectAccess = null;
                effectAccessUnavailable = true;
                reportEffectFallback(exception);
            }
        }

        PotionEffect primary = beacon.getPrimaryEffect();
        if (primary == null) {
            return null;
        }
        PotionEffect secondary = beacon.getSecondaryEffect();
        return new SelectedEffects(
                primary.getType(),
                secondary == null ? null : secondary.getType(),
                primary.getAmplifier() > 0,
                false
        );
    }

    double readConfiguredRange(Beacon beacon) {
        RangeAccess access = rangeAccess;
        if (access == null && !rangeAccessUnavailable) {
            access = discoverRangeAccess(beacon);
            if (access == null) {
                rangeAccessUnavailable = true;
            } else {
                rangeAccess = access;
            }
        }

        if (access != null) {
            try {
                Object tag = access.getSnapshotNbt.invoke(beacon);
                if (access.contains != null
                        && !((Boolean) access.contains.invoke(tag, PAPER_EFFECT_RANGE_TAG))) {
                    return Double.NaN;
                }
                Object value = access.acceptsDefault
                        ? access.getDouble.invoke(tag, PAPER_EFFECT_RANGE_TAG, -1.0D)
                        : access.getDouble.invoke(tag, PAPER_EFFECT_RANGE_TAG);
                return ((Number) value).doubleValue();
            } catch (ReflectiveOperationException | ClassCastException exception) {
                rangeAccess = null;
                rangeAccessUnavailable = true;
            }
        }
        return Double.NaN;
    }

    double readPublicEffectRange(Beacon beacon) {
        try {
            Method method = beacon.getClass().getMethod("getEffectRange");
            return ((Number) method.invoke(beacon)).doubleValue();
        } catch (ReflectiveOperationException | ClassCastException exception) {
            return Double.NaN;
        }
    }

    static boolean blocksFromVanillaValues(
            Material type,
            int lightDampening,
            boolean beaconBeamBlock
    ) {
        return type != Material.TINTED_GLASS
                && type != Material.BEDROCK
                && !beaconBeamBlock
                && lightDampening >= 15;
    }

    static boolean blocksFromBukkitFallback(Material type, boolean occluding) {
        return type != Material.TINTED_GLASS
                && type != Material.BEDROCK
                && type != Material.BARRIER
                && type != Material.SLIME_BLOCK
                && occluding;
    }

    private BeamAccess discoverBeamAccess(Block block, BlockData data) {
        try {
            Method getState = data.getClass().getMethod("getState");
            Object state = getState.invoke(data);
            try {
                Method getLightDampening = state.getClass().getMethod("getLightDampening");
                return new BeamAccess(getState, getLightDampening, null, null);
            } catch (NoSuchMethodException ignored) {
                try {
                    Method getLightBlock = state.getClass().getMethod("getLightBlock");
                    return new BeamAccess(getState, getLightBlock, null, null);
                } catch (NoSuchMethodException secondIgnored) {
                    // Older CraftBukkit runtimes take world and position
                    // arguments, sometimes under an obfuscated method name.
                }
            }

            Method getWorldHandle = block.getWorld().getClass().getMethod("getHandle");
            Method getBlockPosition = block.getClass().getMethod("getPosition");
            Object worldHandle = getWorldHandle.invoke(block.getWorld());
            Object position = getBlockPosition.invoke(block);
            Method contextualLightDampening = findContextualLightDampening(
                    state.getClass(),
                    worldHandle,
                    position
            );
            return new BeamAccess(
                    getState,
                    contextualLightDampening,
                    getWorldHandle,
                    getBlockPosition
            );
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            reportBeamFallback(exception);
            return BeamAccess.UNAVAILABLE;
        }
    }

    private static Method findContextualLightDampening(
            Class<?> stateClass,
            Object worldHandle,
            Object position
    ) throws NoSuchMethodException {
        for (Method method : stateClass.getMethods()) {
            Class<?>[] parameters = method.getParameterTypes();
            if (method.getReturnType() == int.class
                    && parameters.length == 2
                    && parameters[0].isInterface()
                    && parameters[0].isInstance(worldHandle)
                    && parameters[1].isInstance(position)) {
                return method;
            }
        }
        throw new NoSuchMethodException("Could not find the contextual light-dampening method");
    }

    private EffectAccess discoverEffectAccess(Beacon beacon) {
        try {
            Method getBlockEntity;
            try {
                getBlockEntity = beacon.getClass().getMethod("getBlockEntity");
            } catch (NoSuchMethodException ignored) {
                getBlockEntity = beacon.getClass().getMethod("getTileEntity");
            }
            Object handle = getBlockEntity.invoke(beacon);
            if (handle == null) {
                return null;
            }
            Class<?> handleClass = handle.getClass();
            Field primaryPower;
            Field secondaryPower;
            try {
                primaryPower = findPublicField(handleClass, "primaryPower", "m");
                secondaryPower = findPublicField(handleClass, "secondaryPower", "q");
            } catch (NoSuchFieldException ignored) {
                Field[] selectedPowerFields = findSelectedPowerFields(
                        beacon.getClass(),
                        handleClass
                );
                primaryPower = selectedPowerFields[0];
                secondaryPower = selectedPowerFields[1];
            }
            Method converter;
            try {
                converter = findEffectConverter(beacon.getClass(), primaryPower.getType());
            } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                converter = null;
            }
            Method bukkitHandle = null;
            if (converter == null) {
                try {
                    bukkitHandle = findBukkitEffectHandle(primaryPower.getType());
                } catch (NoSuchMethodException ignored) {
                    // Modern holders can still be converted through unwrapKey.
                }
            }
            EffectAccess access = new EffectAccess(
                    getBlockEntity,
                    primaryPower,
                    secondaryPower,
                    converter,
                    bukkitHandle
            );
            return orientSelectedPowerFields(beacon, handle, access);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            reportEffectFallback(exception);
            return null;
        }
    }

    private static Field[] findSelectedPowerFields(Class<?> beaconClass, Class<?> handleClass)
            throws ReflectiveOperationException {
        Field[] fields = handleClass.getFields();
        for (int first = 0; first < fields.length; first++) {
            Field firstField = fields[first];
            if (Modifier.isStatic(firstField.getModifiers()) || firstField.getType() == Object.class) {
                continue;
            }
            for (int second = first + 1; second < fields.length; second++) {
                Field secondField = fields[second];
                if (Modifier.isStatic(secondField.getModifiers())
                        || firstField.getType() != secondField.getType()) {
                    continue;
                }

                boolean convertible;
                try {
                    findEffectConverter(beaconClass, firstField.getType());
                    convertible = true;
                } catch (ClassNotFoundException | NoSuchMethodException ignored) {
                    try {
                        findBukkitEffectHandle(firstField.getType());
                        convertible = true;
                    } catch (NoSuchMethodException secondIgnored) {
                        try {
                            firstField.getType().getMethod("unwrapKey");
                            convertible = true;
                        } catch (NoSuchMethodException thirdIgnored) {
                            convertible = false;
                        }
                    }
                }
                if (convertible) {
                    return new Field[]{firstField, secondField};
                }
            }
        }
        throw new NoSuchFieldException("Could not identify the selected beacon powers");
    }

    private static EffectAccess orientSelectedPowerFields(
            Beacon beacon,
            Object handle,
            EffectAccess access
    ) throws ReflectiveOperationException {
        Object first = access.primaryPower.get(handle);
        Object second = access.secondaryPower.get(handle);
        boolean swap = false;

        PotionEffect publicPrimary = beacon.getPrimaryEffect();
        if (publicPrimary != null) {
            if (first == null && second != null) {
                swap = true;
            } else if (first != null && second != null) {
                PotionEffectType firstType = convertEffect(access, first);
                PotionEffectType secondType = convertEffect(access, second);
                swap = !publicPrimary.getType().equals(firstType)
                        && publicPrimary.getType().equals(secondType);
            }
        }

        return swap
                ? new EffectAccess(
                        access.getBlockEntity,
                        access.secondaryPower,
                        access.primaryPower,
                        access.converter,
                        access.bukkitHandle
                )
                : access;
    }

    private static Field findPublicField(Class<?> owner, String... names) throws NoSuchFieldException {
        for (String name : names) {
            try {
                Field field = owner.getField(name);
                if (!Modifier.isStatic(field.getModifiers())) {
                    return field;
                }
            } catch (NoSuchFieldException ignored) {
                // Try the name used by the next supported mapping generation.
            }
        }
        throw new NoSuchFieldException(owner.getName());
    }

    private static Method findBukkitEffectHandle(Class<?> internalType) throws NoSuchMethodException {
        for (PotionEffectType effect : PotionEffectType.values()) {
            if (effect == null) {
                continue;
            }
            for (String name : new String[]{"getHolder", "getHandle"}) {
                try {
                    Method method = effect.getClass().getMethod(name);
                    if (method.getParameterCount() == 0
                            && internalType == method.getReturnType()) {
                        return method;
                    }
                } catch (NoSuchMethodException ignored) {
                    // Try the other accessor name or effect implementation.
                }
            }
        }
        throw new NoSuchMethodException("Could not map Bukkit potion types to server effects");
    }

    private static Method findEffectConverter(Class<?> beaconClass, Class<?> holderClass)
            throws ClassNotFoundException, NoSuchMethodException {
        String beaconClassName = beaconClass.getName();
        int blockPackage = beaconClassName.indexOf(".block.");
        if (blockPackage < 0) {
            throw new ClassNotFoundException("Could not determine the CraftBukkit package");
        }
        String className = beaconClassName.substring(0, blockPackage)
                + ".potion.CraftPotionEffectType";
        Class<?> converterClass = Class.forName(className, false, beaconClass.getClassLoader());
        for (Method method : converterClass.getMethods()) {
            if (method.getName().equals("minecraftHolderToBukkit")
                    && Modifier.isStatic(method.getModifiers())
                    && method.getParameterCount() == 1
                    && method.getParameterTypes()[0].isAssignableFrom(holderClass)
                    && PotionEffectType.class.isAssignableFrom(method.getReturnType())) {
                return method;
            }
        }
        throw new NoSuchMethodException(className + "#minecraftHolderToBukkit");
    }

    private static PotionEffectType convertEffect(EffectAccess access, Object internalEffect)
            throws ReflectiveOperationException {
        if (access.converter != null) {
            Object converted = access.converter.invoke(null, internalEffect);
            if (converted instanceof PotionEffectType) {
                return (PotionEffectType) converted;
            }
        }

        if (access.bukkitHandle != null) {
            for (PotionEffectType effect : PotionEffectType.values()) {
                if (effect != null && internalEffect.equals(access.bukkitHandle.invoke(effect))) {
                    return effect;
                }
            }
        }

        Method unwrapKey = internalEffect.getClass().getMethod("unwrapKey");
        Object optional = unwrapKey.invoke(internalEffect);
        if (!(optional instanceof Optional)) {
            return null;
        }
        Object resourceKey = ((Optional<?>) optional).orElse(null);
        if (resourceKey == null) {
            return null;
        }

        Method identifier;
        try {
            identifier = resourceKey.getClass().getMethod("identifier");
        } catch (NoSuchMethodException ignored) {
            identifier = resourceKey.getClass().getMethod("location");
        }
        NamespacedKey key = NamespacedKey.fromString(String.valueOf(identifier.invoke(resourceKey)));
        if (key == null) {
            return null;
        }
        try {
            Method getByKey = PotionEffectType.class.getMethod("getByKey", NamespacedKey.class);
            Object converted = getByKey.invoke(null, key);
            return converted instanceof PotionEffectType ? (PotionEffectType) converted : null;
        } catch (NoSuchMethodException ignored) {
            return PotionEffectType.getByName(key.getKey());
        }
    }

    private static RangeAccess discoverRangeAccess(Beacon beacon) {
        try {
            Method getSnapshotNbt = beacon.getClass().getMethod("getSnapshotNBT");
            try {
                Method getDoubleOr = getSnapshotNbt.getReturnType().getMethod(
                        "getDoubleOr",
                        String.class,
                        double.class
                );
                return new RangeAccess(getSnapshotNbt, getDoubleOr, true, null);
            } catch (NoSuchMethodException ignored) {
                Method getDouble = getSnapshotNbt.getReturnType().getMethod(
                        "getDouble",
                        String.class
                );
                if (getDouble.getReturnType() == double.class
                        || Number.class.isAssignableFrom(getDouble.getReturnType())) {
                    Method contains;
                    try {
                        contains = getSnapshotNbt.getReturnType().getMethod("contains", String.class);
                    } catch (NoSuchMethodException secondIgnored) {
                        contains = getSnapshotNbt.getReturnType().getMethod("hasKey", String.class);
                    }
                    return new RangeAccess(getSnapshotNbt, getDouble, false, contains);
                }
                return null;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return null;
        }
    }

    private void reportBeamFallback(Throwable exception) {
        if (!reportedBeamFallback) {
            reportedBeamFallback = true;
            plugin.getLogger().log(
                    Level.FINE,
                    "Exact server beam data is unavailable; using the Bukkit compatibility rule.",
                    unwrap(exception)
            );
        }
    }

    private void reportEffectFallback(Throwable exception) {
        if (!reportedEffectFallback) {
            reportedEffectFallback = true;
            plugin.getLogger().log(
                    Level.FINE,
                    "Exact server beacon selections are unavailable; using the Bukkit API.",
                    unwrap(exception)
            );
        }
    }

    private static Throwable unwrap(Throwable exception) {
        if (exception instanceof InvocationTargetException) {
            Throwable cause = ((InvocationTargetException) exception).getCause();
            if (cause != null) {
                return cause;
            }
        }
        return exception;
    }

    static final class SelectedEffects {
        private final PotionEffectType primary;
        private final PotionEffectType secondary;
        private final boolean upgradedPrimary;
        private final boolean exact;

        SelectedEffects(
                PotionEffectType primary,
                PotionEffectType secondary,
                boolean upgradedPrimary,
                boolean exact
        ) {
            this.primary = primary;
            this.secondary = secondary;
            this.upgradedPrimary = upgradedPrimary;
            this.exact = exact;
        }

        PotionEffectType primary() {
            return primary;
        }

        PotionEffectType secondary() {
            return secondary;
        }

        boolean upgradedPrimary() {
            return upgradedPrimary;
        }

        boolean exact() {
            return exact;
        }
    }

    private static final class BeamAccess {
        private static final BeamAccess UNAVAILABLE = new BeamAccess(null, null, null, null);

        private final Method getState;
        private final Method getLightDampening;
        private final Method getWorldHandle;
        private final Method getBlockPosition;

        private BeamAccess(
                Method getState,
                Method getLightDampening,
                Method getWorldHandle,
                Method getBlockPosition
        ) {
            this.getState = getState;
            this.getLightDampening = getLightDampening;
            this.getWorldHandle = getWorldHandle;
            this.getBlockPosition = getBlockPosition;
        }
    }

    private static final class EffectAccess {
        private final Method getBlockEntity;
        private final Field primaryPower;
        private final Field secondaryPower;
        private final Method converter;
        private final Method bukkitHandle;

        private EffectAccess(
                Method getBlockEntity,
                Field primaryPower,
                Field secondaryPower,
                Method converter,
                Method bukkitHandle
        ) {
            this.getBlockEntity = getBlockEntity;
            this.primaryPower = primaryPower;
            this.secondaryPower = secondaryPower;
            this.converter = converter;
            this.bukkitHandle = bukkitHandle;
        }
    }

    private static final class RangeAccess {
        private final Method getSnapshotNbt;
        private final Method getDouble;
        private final boolean acceptsDefault;
        private final Method contains;

        private RangeAccess(
                Method getSnapshotNbt,
                Method getDouble,
                boolean acceptsDefault,
                Method contains
        ) {
            this.getSnapshotNbt = getSnapshotNbt;
            this.getDouble = getDouble;
            this.acceptsDefault = acceptsDefault;
            this.contains = contains;
        }
    }
}
