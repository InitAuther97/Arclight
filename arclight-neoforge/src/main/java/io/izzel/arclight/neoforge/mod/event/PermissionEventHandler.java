package io.izzel.arclight.neoforge.mod.event;

import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.i18n.ArclightConfig;
import io.izzel.arclight.neoforge.mod.permission.ArclightPermissionHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForgeConfig;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.handler.DefaultPermissionHandler;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandlerFactory;

import java.util.stream.Collectors;

public class PermissionEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRegisterHandler(PermissionGatherEvent.Handler event) {
        if (!ArclightConfig.spec().getCompat().isForwardPermission()) {
            return;
        }
        final var handlerCfg = NeoForgeConfig.SERVER.permissionHandler.get();
        final var arclight = ArclightPermissionHandler.IDENTIFIER.toString();
        if (!DefaultPermissionHandler.IDENTIFIER.toString().equals(handlerCfg)) {
            ArclightServer.LOGGER.warn("Forwarding NeoForge permission to Bukkit under the presence of other permission API providers!");
            ArclightServer.LOGGER.warn("This typically leads to strange behaviors. This is not suggested unless you know what you are doing!");
            ArclightServer.LOGGER.warn("Permission handlers present: {}", event.getAvailablePermissionHandlerFactories()
                    .keySet()
                    .stream()
                    .map(ResourceLocation::toString)
                    .collect(Collectors.joining(",")));
            ArclightServer.LOGGER.warn("Currently configured to use: {}", handlerCfg);
            ArclightServer.LOGGER.warn("Fallback permission provider is set to: {}", handlerCfg);
        }
        IPermissionHandlerFactory fallback = event.getAvailablePermissionHandlerFactories().get(ResourceLocation.parse(handlerCfg));
        if (fallback == null) {
            ArclightServer.LOGGER.error("Configured permission handler is not found, will use {}", DefaultPermissionHandler.IDENTIFIER);
            fallback = event.getAvailablePermissionHandlerFactories().get(DefaultPermissionHandler.IDENTIFIER);
        }
        event.addPermissionHandler(ArclightPermissionHandler.IDENTIFIER, ArclightPermissionHandler.factory(fallback));
        NeoForgeConfig.SERVER.permissionHandler.set(arclight);
    }
}
