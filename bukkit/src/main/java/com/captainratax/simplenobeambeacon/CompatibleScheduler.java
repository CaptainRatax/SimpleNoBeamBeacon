package com.captainratax.simplenobeambeacon;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

final class CompatibleScheduler implements SchedulerFacade {
    private static final String FOLIA_SERVER_CLASS = "io.papermc.paper.threadedregions.RegionizedServer";

    private final Plugin plugin;
    private final boolean folia;
    private final Method getRegionScheduler;
    private final Method getEntityScheduler;
    private final Method regionRunAtFixedRate;
    private final Method regionRunDelayed;
    private final Method entityRunAtFixedRate;
    private final Method scheduledTaskCancel;

    CompatibleScheduler(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.folia = classExists(FOLIA_SERVER_CLASS);

        if (folia) {
            try {
                this.getRegionScheduler = Bukkit.class.getMethod("getRegionScheduler");
                this.getEntityScheduler = Entity.class.getMethod("getScheduler");
                Class<?> regionScheduler = Class.forName(
                        "io.papermc.paper.threadedregions.scheduler.RegionScheduler"
                );
                Class<?> entityScheduler = Class.forName(
                        "io.papermc.paper.threadedregions.scheduler.EntityScheduler"
                );
                Class<?> scheduledTask = Class.forName(
                        "io.papermc.paper.threadedregions.scheduler.ScheduledTask"
                );
                this.regionRunAtFixedRate = regionScheduler.getMethod(
                        "runAtFixedRate",
                        Plugin.class,
                        Location.class,
                        Consumer.class,
                        long.class,
                        long.class
                );
                this.regionRunDelayed = regionScheduler.getMethod(
                        "runDelayed",
                        Plugin.class,
                        Location.class,
                        Consumer.class,
                        long.class
                );
                this.entityRunAtFixedRate = entityScheduler.getMethod(
                        "runAtFixedRate",
                        Plugin.class,
                        Consumer.class,
                        Runnable.class,
                        long.class,
                        long.class
                );
                this.scheduledTaskCancel = scheduledTask.getMethod("cancel");
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Folia was detected, but its scheduler API is unavailable", exception);
            }
        } else {
            this.getRegionScheduler = null;
            this.getEntityScheduler = null;
            this.regionRunAtFixedRate = null;
            this.regionRunDelayed = null;
            this.entityRunAtFixedRate = null;
            this.scheduledTaskCancel = null;
        }
    }

    @Override
    public void runAtLocation(
            Location location,
            long initialDelayTicks,
            long periodTicks,
            RepeatingTask task
    ) {
        if (!folia) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!task.run()) {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, initialDelayTicks, periodTicks);
            return;
        }

        Object scheduler = invoke(getRegionScheduler, null);
        Consumer<Object> callback = scheduledTask -> {
            if (!task.run()) {
                cancelFoliaTask(scheduledTask);
            }
        };
        invoke(
                regionRunAtFixedRate,
                scheduler,
                plugin,
                location,
                callback,
                initialDelayTicks,
                periodTicks
        );
    }

    @Override
    public void runForPlayer(
            Player player,
            long initialDelayTicks,
            long periodTicks,
            RepeatingTask task,
            Runnable retired
    ) {
        if (!folia) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || !task.run()) {
                        cancel();
                        retired.run();
                    }
                }
            }.runTaskTimer(plugin, initialDelayTicks, periodTicks);
            return;
        }

        Object scheduler = invoke(getEntityScheduler, player);
        Consumer<Object> callback = scheduledTask -> {
            if (!task.run()) {
                cancelFoliaTask(scheduledTask);
                retired.run();
            }
        };
        Object scheduledTask = invoke(
                entityRunAtFixedRate,
                scheduler,
                plugin,
                callback,
                retired,
                initialDelayTicks,
                periodTicks
        );
        if (scheduledTask == null) {
            retired.run();
        }
    }

    @Override
    public void runLater(Location location, long delayTicks, Runnable task) {
        if (!folia) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return;
        }

        Object scheduler = invoke(getRegionScheduler, null);
        Consumer<Object> callback = ignored -> task.run();
        invoke(
                regionRunDelayed,
                scheduler,
                plugin,
                location,
                callback,
                delayTicks
        );
    }

    @Override
    public boolean isFolia() {
        return folia;
    }

    private void cancelFoliaTask(Object scheduledTask) {
        try {
            scheduledTaskCancel.invoke(scheduledTask);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            plugin.getLogger().log(Level.WARNING, "Could not cancel a Folia task", exception);
        }
    }

    private static Object invoke(Method method, Object receiver, Object... arguments) {
        try {
            return method.invoke(receiver, arguments);
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Could not call the server scheduler API", unwrap(exception));
        }
    }

    private static Throwable unwrap(ReflectiveOperationException exception) {
        if (exception instanceof InvocationTargetException) {
            Throwable cause = ((InvocationTargetException) exception).getCause();
            if (cause != null) {
                return cause;
            }
        }
        return exception;
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, CompatibleScheduler.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
