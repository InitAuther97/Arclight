package io.izzel.arclight.boot;

import io.izzel.arclight.api.ArclightVersion;
import io.izzel.arclight.i18n.ArclightLocale;
import org.apache.logging.log4j.LogManager;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/*
 * Loaded by ArclightBootstrapClassLoader
 * dirtyHacks() moved to CL
 */
public class AbstractBootstrap {
    // Gson TypeAdapters.ENUM_FACTORY set to owned EnumTypeFactory
    // com.mojang.brigadier.tree.CommandNode transformed
    // protected void dirtyHacks() throws Exception {}

    protected void setupMod() throws Exception {
        ArclightVersion.setVersion(ArclightVersion.TRIALS);
        var logger = LogManager.getLogger("Arclight");
        try (InputStream stream = getClass().getModule().getResourceAsStream("/META-INF/MANIFEST.MF")) {
            Manifest manifest = new Manifest(stream);
            Attributes attributes = manifest.getMainAttributes();
            String version = attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            extract(getClass().getModule().getResourceAsStream("/common.jar"), version);
            String buildTime = attributes.getValue("Implementation-Timestamp");
            logger.info(ArclightLocale.getInstance().get("logo"),
                ArclightLocale.getInstance().get("release-name." + ArclightVersion.current().getReleaseName()), version, buildTime);
        }
    }

    private void extract(InputStream path, String version) throws Exception {
        System.setProperty("arclight.version", version);
        var dir = Paths.get(".arclight", "mod_file");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        var mod = dir.resolve(version + ".jar");
        if (!Files.exists(mod) || Boolean.getBoolean("arclight.alwaysExtract")) {
            for (Path old : Files.list(dir).toList()) {
                Files.delete(old);
            }
            Files.copy(path, mod);
        }
    }
}
