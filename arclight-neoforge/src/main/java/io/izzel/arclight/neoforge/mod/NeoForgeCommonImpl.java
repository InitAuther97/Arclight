package io.izzel.arclight.neoforge.mod;

import com.google.common.graph.Graph;
import com.google.common.graph.Graphs;
import cpw.mods.modlauncher.ClassTransformer;
import cpw.mods.modlauncher.TransformingClassLoader;
import io.izzel.arclight.api.Unsafe;
import io.izzel.arclight.common.mod.ArclightCommon;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.util.FastClassNotFoundException;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLLoader;
import org.objectweb.asm.ClassReader;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.regex.Pattern;

public class NeoForgeCommonImpl implements ArclightCommon.Api {

    private static final MethodHandle MH_TRANSFORM;
    static final Pattern INVALID_DIST = Pattern.compile("Attempted to load class ([\\w.]*) for invalid dist DEDICATED_SERVER");

    static {
        try {
            ClassLoader classLoader = NeoForgeCommonImpl.class.getClassLoader();
            Field classTransformer = TransformingClassLoader.class.getDeclaredField("classTransformer");
            classTransformer.setAccessible(true);
            ClassTransformer transformer = (ClassTransformer) classTransformer.get(classLoader);
            Method transform = transformer.getClass().getDeclaredMethod("transform", byte[].class, String.class, String.class);
            MH_TRANSFORM = Unsafe.lookup().unreflect(transform).bindTo(transformer);
        } catch (Throwable t) {
            throw new IllegalStateException("Unknown modlauncher version", t);
        }
    }

    @Override
    public byte[] platformRemapClass(byte[] cl) {
        String className = new ClassReader(cl).getClassName();
        try {
            return (byte[]) MH_TRANSFORM.invokeExact(cl, className.replace('/', '.'), "source");
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isModLoaded(String modid) {
        return ModList.get() != null ? ModList.get().isLoaded(modid) : FMLLoader.getLoadingModList().getModFileById(modid) != null;
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
