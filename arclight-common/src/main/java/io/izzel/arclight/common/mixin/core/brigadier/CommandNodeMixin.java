package io.izzel.arclight.common.mixin.core.brigadier;

import com.mojang.brigadier.tree.CommandNode;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(CommandNode.class)
public abstract class CommandNodeMixin {

    @Shadow @Final private Map<String, ?> children;
    @Shadow @Final private Map<String, ?> literals;
    @Shadow @Final private Map<String, ?> arguments;
    @Unique
    private static volatile CommandNode<?> CURRENT_COMMAND;

    @Redirect(method = "canUse", at = @At(value = "INVOKE", target = "Ljava/util/function/Predicate;test(Ljava/lang/Object;)Z"), remap = false)
    private<T> boolean wrapTest(Predicate<T> instance, T t) {
        CURRENT_COMMAND = (CommandNode<?>)(Object)this;
        System.out.println("CommandNode test");
        try {
            return instance.test(t);
        } finally {
            CURRENT_COMMAND = null;
        }
    }

    @Unique
    public void removeCommand(String command) {
        this.children.remove(command);
        this.literals.remove(command);
        this.arguments.remove(command);
    }
}
