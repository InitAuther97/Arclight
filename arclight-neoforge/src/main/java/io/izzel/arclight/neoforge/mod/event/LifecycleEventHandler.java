package io.izzel.arclight.neoforge.mod.event;

import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.neoforge.mod.permission.ArclightPermissionHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.server.permission.PermissionAPI;

public class LifecycleEventHandler {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        final var fb = ArclightPermissionHandler.fallback;
        if (ArclightConfig.spec().getCompat().isForwardPermission()) {
            if (fb != null) {
                ArclightServer.LOGGER.info("Forwarding NeoForge permission[{}] to Bukkit.", fb);
            } else {
                ArclightServer.LOGGER.error("Permission handler is replaced by some other permission providers. Currently active:{}", PermissionAPI.getActivePermissionHandler());
            }
        }
    }
}
