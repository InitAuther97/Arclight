package io.izzel.arclight.common.mod.server.event;

import com.google.gson.Gson;
import cpw.mods.modlauncher.Launcher;
import io.izzel.arclight.common.mod.ArclightMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;

import java.util.stream.Collectors;

public abstract class ArclightEventDispatcherRegistry {

    public static void registerAllEventDispatchers() {
        MinecraftForge.EVENT_BUS.register(new BlockBreakEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new BlockPlaceEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityPotionEffectEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new EntityTeleportEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new ItemEntityEventDispatcher());
        MinecraftForge.EVENT_BUS.register(new WorldEventDispatcher());
        MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> {
            ArclightMod.LOGGER.info("ClassLoader comparison: GSON {}, event {}", Gson.class.getClassLoader(), event.getClass().getClassLoader());
            ArclightMod.LOGGER.info("Launcher classloader: {}", Launcher.class.getClassLoader());
            ArclightMod.LOGGER.info("Launcher LayerManager: {}", Launcher.INSTANCE.findLayerManager().get());
            var gsonCp = Gson.class.getModule().getLayer().modules()
                    .stream()
                    .map(Module::getName)
                    .collect(Collectors.joining(","));
            var forgeCp = event.getClass().getModule().getLayer().modules()
                    .stream()
                    .map(Module::getName)
                    .collect(Collectors.joining(","));
            ArclightMod.LOGGER.info("Gson Classpath: {}", gsonCp);
            ArclightMod.LOGGER.info("Forge Classpath: {}", forgeCp);
        });
        ArclightMod.LOGGER.info("registry.forge-event");
    }

}
