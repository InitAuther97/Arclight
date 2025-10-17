package io.izzel.arclight.forge.mod.event;

import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.forge.mod.permission.ArclightPermissionHandler;
import io.izzel.arclight.i18n.ArclightConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfig;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.permission.events.PermissionGatherEvent;
import net.minecraftforge.server.permission.handler.DefaultPermissionHandler;
import net.minecraftforge.server.permission.handler.IPermissionHandlerFactory;

import java.util.stream.Collectors;

public class PermissionEventHandler {

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRegisterPermission(PermissionGatherEvent.Handler event) {
        if (!ArclightConfig.spec().getCompat().isForwardPermission()) {
            return;
        }
        final var handlerCfg = ForgeConfig.SERVER.permissionHandler.get();
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
        ForgeConfig.SERVER.permissionHandler.set(arclight);
    }
}
