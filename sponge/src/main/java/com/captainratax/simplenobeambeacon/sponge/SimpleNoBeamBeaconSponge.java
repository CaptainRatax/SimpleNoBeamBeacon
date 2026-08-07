package com.captainratax.simplenobeambeacon.sponge;

import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent;
import org.spongepowered.plugin.builtin.jvm.Plugin;

@Plugin("simplenobeambeacon")
public final class SimpleNoBeamBeaconSponge {
    @Listener
    public void onConstructPlugin(ConstructPluginEvent event) {
        // The behaviour itself is installed at bootstrap time by the server-side mixin.
    }
}
