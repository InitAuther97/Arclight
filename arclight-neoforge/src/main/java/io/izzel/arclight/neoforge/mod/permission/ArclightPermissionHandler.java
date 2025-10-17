package io.izzel.arclight.neoforge.mod.permission;

import io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge;
import io.izzel.arclight.common.mod.server.permission.ArclightPermissibleBase;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandler;
import net.neoforged.neoforge.server.permission.handler.IPermissionHandlerFactory;
import net.neoforged.neoforge.server.permission.nodes.PermissionDynamicContext;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;
import org.bukkit.Bukkit;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ArclightPermissionHandler implements IPermissionHandler {

    public static final ResourceLocation IDENTIFIER = ResourceLocation.fromNamespaceAndPath("arclight", "permission");
    public static ResourceLocation fallback;

    public static IPermissionHandlerFactory factory(IPermissionHandlerFactory delegate) {
        return nodes -> new ArclightPermissionHandler(delegate.create(nodes));
    }

    private final IPermissionHandler delegate;

    public ArclightPermissionHandler(IPermissionHandler delegate) {
        Objects.requireNonNull(delegate, "permission handler delegate");
        this.delegate = delegate;
        fallback = delegate.getIdentifier();
    }

    @Override
    public ResourceLocation getIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public Set<PermissionNode<?>> getRegisteredNodes() {
        return delegate.getRegisteredNodes();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getPermission(ServerPlayer player, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        final var bukkit = ((ServerPlayerEntityBridge) player).bridge$getBukkitEntity();
        final var perm = node.getNodeName();
        if (node.getType() == PermissionTypes.BOOLEAN && ArclightPermissibleBase.isKnownPermission(bukkit, perm)) {
            return (T) Boolean.valueOf(bukkit.hasPermission(perm));
        } else {
            return delegate.getPermission(player, node, context);
        }
    }

    @Override
    public <T> T getOfflinePermission(UUID uuid, PermissionNode<T> node, PermissionDynamicContext<?>... context) {
        final var player = Bukkit.getPlayer(uuid);
        final var perm = node.getNodeName();
        if (player != null && node.getType() == PermissionTypes.BOOLEAN && ArclightPermissibleBase.isKnownPermission(player, perm)) {
            return (T) Boolean.valueOf(player.hasPermission(perm));
        } else {
            return delegate.getOfflinePermission(uuid, node, context);
        }
    }
}
