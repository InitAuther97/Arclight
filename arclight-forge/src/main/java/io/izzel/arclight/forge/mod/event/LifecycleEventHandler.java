package io.izzel.arclight.forge.mod.event;

import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.forge.mod.permission.ArclightPermissionHandler;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.PermissionAPI;

public class LifecycleEventHandler {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        final var fb = ArclightPermissionHandler.fallback;
        if (ArclightConfig.spec().getCompat().isForwardPermission()) {
            if (fb != null) {
                ArclightServer.LOGGER.info("Forwarding Forge permission[{}] to Bukkit.", fb);
            } else {
                ArclightServer.LOGGER.error("Permission handler is replaced by some other permission providers. Currently active:{}", PermissionAPI.getActivePermissionHandler());
            }
        }
    }
}
