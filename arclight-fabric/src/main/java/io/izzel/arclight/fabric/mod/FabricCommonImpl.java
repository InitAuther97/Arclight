package io.izzel.arclight.fabric.mod;

import com.google.common.graph.Graph;
import com.google.common.graph.Graphs;
import io.izzel.arclight.common.mod.ArclightCommon;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.util.FastClassNotFoundException;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.transformer.FabricTransformer;
import org.objectweb.asm.ClassReader;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FabricCommonImpl implements ArclightCommon.Api {

    static final Pattern INVALID_DIST = Pattern.compile("Cannot load class ([\\w.]*) in environment type SERVER");

    @Override
    public byte[] platformRemapClass(byte[] cl) {
        var name = new ClassReader(cl).getClassName();
        var bytes = FabricTransformer.transform(false, EnvType.SERVER, name.replace('/', '.'), cl);
        bytes = ((IMixinTransformer) MixinEnvironment.getCurrentEnvironment().getActiveTransformer()).transformClassBytes(name, name, bytes);
        return bytes;
    }

    @Override
    public boolean isModLoaded(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    @Override
    public <T> Set<T> guavaReachableNodes(Graph<T> graph, T node) {
        return Graphs.reachableNodes(graph, node);
    }

    @Override
    public void rethrowIfNotPresent(RuntimeException e) throws TypeNotPresentException {
        final var msg = e.getMessage();
        if (msg == null) throw e;
        final var matcher = INVALID_DIST.matcher(msg);
        if (!matcher.find()) throw e;
        final var name = matcher.group(1);
        ArclightServer.LOGGER.warn("Remapper: attempt to reflectively access {} which is not available in the server environment", name, e);
        throw new TypeNotPresentException(name, new FastClassNotFoundException(name));
    }
}