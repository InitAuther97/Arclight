package io.izzel.arclight.boot.mod;

import cpw.mods.jarhandling.JarMetadata;
import cpw.mods.jarhandling.SecureJar;
import cpw.mods.jarhandling.impl.SimpleJarMetadata;
import net.minecraftforge.fml.loading.moddiscovery.AbstractJarFileModLocator;
import net.minecraftforge.fml.loading.moddiscovery.ModFileParser;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.forgespi.locating.IModProvider;
import net.minecraftforge.forgespi.locating.ModFileFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.lang.Class.forName;

public class ArclightLocator_Forge extends AbstractJarFileModLocator {

    private final IModFile arclight;
    private final IModFile gson;

    private final MethodHandles.Lookup mhLookup = MethodHandles.lookup();
    private final ModFileConstructor newModFile = new ModFileConstructor(mhLookup);

    public ArclightLocator_Forge() {
        ModBootstrap.run();
        this.arclight = loadArclight();
        this.gson = loadGson();
    }

    @Override
    public List<ModFileOrException> scanMods() {
        ArclightJarInJarAdaptor.inject();
        return List.of(
                new ModFileOrException(arclight, null),
                new ModFileOrException(gson, null)
        );
    }

    @Override
    public Stream<Path> scanCandidates() {
        return Stream.empty();
    }

    @Override
    public String name() {
        return "arclight";
    }

    @Override
    public void initArguments(Map<String, ?> arguments) {
    }

    protected IModFile loadArclight() {
        var version = System.getProperty("arclight.version");
        var path = Paths.get(".arclight", "mod_file", version + ".jar");
        return newModFile.create(SecureJar.from(it -> excludePackages(it, version), path), this, ModFileParser::modsTomlParser, "MOD");
    }

    protected IModFile loadGson() {
        return newModFile.create(SecureJar.from(Paths.get(".arclight", "gson.jar")), this, this::manifestParser, "GAMELIBRARY");
    }

    private static final Set<String> EXCLUDES = Set.of(
        "net.minecraft.world.level.block"
    );

    private JarMetadata excludePackages(SecureJar secureJar, String version) {
        secureJar.getPackages().removeIf(it -> EXCLUDES.stream().anyMatch(it::startsWith));
        return new SimpleJarMetadata("arclight", version.substring(version.indexOf('-') + 1), secureJar.getPackages(), List.of());
    }

    static class ModFileConstructor {
        private final MethodHandle ctor;
        ModFileConstructor(MethodHandles.Lookup lookup) {
            try {
                var cl = forName("net.minecraftforge.fml.loading.moddiscovery.ModFile");
                ctor = lookup.findConstructor(cl, MethodType.methodType(void.class, SecureJar.class, IModProvider.class, ModFileFactory.ModFileInfoParser.class, String.class));
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        public IModFile create(SecureJar jar, IModProvider provider, ModFileFactory.ModFileInfoParser parser, String type) {
            try {
                return (IModFile) ctor.invoke(jar, provider, parser, type);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }
}
