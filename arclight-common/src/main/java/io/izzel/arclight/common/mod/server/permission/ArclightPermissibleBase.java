package io.izzel.arclight.common.mod.server.permission;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.jetbrains.annotations.Nullable;

/// TODO: Register modded permission nodes to Bukkit, if possible?
public class ArclightPermissibleBase extends PermissibleBase {
    public ArclightPermissibleBase(@Nullable ServerOperator opable) {
        super(opable);
    }

    public static boolean isKnownPermission(CommandSender sender, String perm) {
        return sender.isPermissionSet(perm) || Bukkit.getPluginManager().getPermission(perm) != null;
    }
}
