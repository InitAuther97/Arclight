package io.izzel.arclight.common.mod.mixins;

import io.izzel.arclight.common.mod.mixins.annotation.LoadIfProperty;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import java.util.List;

public class LoadIfPropertyProcessor {

    private static final String TYPE = Type.getDescriptor(LoadIfProperty.class);

    static boolean shouldApply(ClassNode node) {
        for (var ann : node.invisibleAnnotations) {
            if (ann.desc.equals(TYPE)) {
                for (String prop : (List<String>) ann.values.get(1)) {
                    if (System.getProperty(prop) == null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
}
