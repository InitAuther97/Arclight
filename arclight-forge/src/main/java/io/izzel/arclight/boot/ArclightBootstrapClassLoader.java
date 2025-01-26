package io.izzel.arclight.boot;

import cpw.mods.cl.ModuleClassLoader;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.lang.module.Configuration;
import java.util.List;
import java.util.Map;

/*
 * Loaded by AppClassLoader.
 * Load and transform CommandNode and TypeAdapters.
 * Name: MC-BOOTSTRAP
 */
@SuppressWarnings("unused")
public class ArclightBootstrapClassLoader extends ModuleClassLoader {

    public ArclightBootstrapClassLoader(String name, Configuration configuration, List<ModuleLayer> parentLayers) {
        super(name, configuration, parentLayers);
    }

    @Override
    protected byte[] maybeTransformClassBytes(byte[] bytes, String name, String context) {
        // Use endsWith to consider shadowed libraries?
        // Libraries that belong to Mojang won't be shadowed
        if (name.endsWith("com.google.gson.internal.bind.TypeAdapters")) {
            //return transformTypeAdapters(name, bytes);
            return bytes;
        } else if ("com.mojang.brigadier.tree.CommandNode".equals(name)) {
            return transformCommandNode(name, bytes);
        } else if (name.endsWith("com.google.gson.TypeAdapter")) {
            return transformTypeAdapter(name, bytes);
        }
        return super.maybeTransformClassBytes(bytes, name, context);
    }

    private byte[] transformTypeAdapter(String name, byte[] bytes) {
        // Read class.
        var clazz = new ClassNode();
        new ClassReader(bytes).accept(clazz, 0);

        // Add field:
        // public static TypeAdapterFactory ARCLIGHT_ENUM_FACTORY;
        {
            FieldNode factory = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "ARCLIGHT_ENUM_FACTORY", "Lcom/google/gson/TypeAdapterFactory;", null, null);
            clazz.fields.add(factory);
        }

        // Save transformed class.
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        clazz.accept(writer);
        return writer.toByteArray();
    }

    private byte[] transformCommandNode(String name, byte[] bytes) {
        // Read class.
        var clazz = new ClassNode();
        new ClassReader(bytes).accept(clazz, 0);

        // Add field:
        // public static volatile CommandNode CURRENT_COMMAND;
        FieldNode currentCommand;
        {
            currentCommand = new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_VOLATILE, "CURRENT_COMMAND", "Lcom/mojang/brigadier/tree/CommandNode;", null, null);
            clazz.fields.add(currentCommand);
        }

        // Find and transform canUse.
        // Raw: requirements.test(source);
        // Modified:
        // CURRENT_COMMAND = this;
        // requirements.test(source);
        // CURRENT_COMMAND = null;
        {
            var findMethod = false;
            var findInvoke = false;
            for (var method : clazz.methods) {
                if (method.name.equals("canUse")) {
                    findMethod = true;
                    for (var instruction : method.instructions) {
                        if (instruction.getOpcode() == Opcodes.INVOKEINTERFACE || instruction.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                            findInvoke = true;
                            var assign = new InsnList();
                            assign.add(new VarInsnNode(Opcodes.ALOAD, 0));
                            assign.add(new FieldInsnNode(Opcodes.PUTSTATIC, "com/mojang/brigadier/tree/CommandNode", currentCommand.name, currentCommand.desc));
                            method.instructions.insertBefore(instruction, assign);
                            var reset = new InsnList();
                            reset.add(new InsnNode(Opcodes.ACONST_NULL));
                            reset.add(new FieldInsnNode(Opcodes.PUTSTATIC, "com/mojang/brigadier/tree/CommandNode", currentCommand.name, currentCommand.desc));
                            method.instructions.insert(instruction, assign);
                            break;
                        }
                    }
                }
            }
            if (!findMethod) {
                throw new RuntimeException("Cannot transform "+name+": canUse(...) not found");
            } else if (!findInvoke) {
                throw new RuntimeException("Cannot transform "+name+": requirements.test(...) not found");
            }
        }

        // Add method:
        /*
         * public void removeCommand(String command) {
         *     this.children.remove(command);
         *     this.literals.remove(command);
         *     this.arguments.remove(command);
         * }
         */
        {
            var removeCommand = new MethodNode();
            removeCommand.access = Opcodes.ACC_PUBLIC;
            removeCommand.name = "removeCommand";
            removeCommand.desc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getType(String.class));
            var codes = removeCommand.instructions;
            {
                codes.add(new LabelNode());
                codes.add(new VarInsnNode(Opcodes.ALOAD, 0));
                codes.add(new FieldInsnNode(Opcodes.GETFIELD, "com/mojang/brigadier/tree/CommandNode", "children", Type.getDescriptor(Map.class)));
                codes.add(new VarInsnNode(Opcodes.ALOAD, 1));
                codes.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class), "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", true));
                codes.add(new InsnNode(Opcodes.POP));
            }
            {
                codes.add(new LabelNode());
                codes.add(new VarInsnNode(Opcodes.ALOAD, 0));
                codes.add(new FieldInsnNode(Opcodes.GETFIELD, "com/mojang/brigadier/tree/CommandNode", "literals", Type.getDescriptor(Map.class)));
                codes.add(new VarInsnNode(Opcodes.ALOAD, 1));
                codes.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class), "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", true));
                codes.add(new InsnNode(Opcodes.POP));
            }
            {
                codes.add(new LabelNode());
                codes.add(new VarInsnNode(Opcodes.ALOAD, 0));
                codes.add(new FieldInsnNode(Opcodes.GETFIELD, "com/mojang/brigadier/tree/CommandNode", "arguments", Type.getDescriptor(Map.class)));
                codes.add(new VarInsnNode(Opcodes.ALOAD, 1));
                codes.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, Type.getInternalName(Map.class), "remove", "(Ljava/lang/Object;)Ljava/lang/Object;", true));
                codes.add(new InsnNode(Opcodes.POP));
            }
            codes.add(new InsnNode(Opcodes.RETURN));
            clazz.methods.add(removeCommand);
        }

        // Save transformed class.
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        clazz.accept(writer);
        return writer.toByteArray();
    }

    private byte[] transformTypeAdapters(String name, byte[] bytes) {
        // Read class.
        var clazz = new ClassNode();
        new ClassReader(bytes).accept(clazz, 0);

        // Find <clinit>.
        MethodNode clinit = null;
        for (var method: clazz.methods) {
            if ("<clinit>".equals(method.name)) {
                clinit = method;
            }
        }
        if (clinit == null) {
            throw new RuntimeException("Cannot transform "+name+": <clinit> not found");
        }

        // Find and transform NEW & INVOKESPECIAL for TypeAdapters$29.
        // Raw: ENUM_FACTORY = new TypeAdapters$29();
        // Modified: ENUM_FACTORY = EnumTypeAdapter.ARCLIGHT$ENUM_TYPE_FACTORY()
        final var rawName = "com/google/gson/internal/bind/TypeAdapters$29";
        final var targetName = "io/izzel/arclight/boot/EnumTypeFactory";
        boolean findNew = false;
        boolean findInit = false;
        for (var node: clinit.instructions) {
            if (!findNew && node instanceof TypeInsnNode typed
                    && Opcodes.NEW == typed.getOpcode()
                    && rawName.equals(typed.desc)) {
                typed.desc = targetName;
                findNew = true;
            } else if (findNew && node instanceof MethodInsnNode invoke
                    && rawName.equals(invoke.owner)
                    && "<init>".equals(invoke.name)) {
                invoke.owner = targetName;
                findInit = true;
                break;
            }
        }
        if (!findNew) {
            throw new RuntimeException("Cannot transform "+rawName+": NEW TypeAdapters$29 not found");
        } else if (!findInit) {
            throw new RuntimeException("Cannot transform "+rawName+": TypeAdapters$29.<init> not found");
        }

        // Save transformed class.
        var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        clazz.accept(writer);
        return writer.toByteArray();
    }
}
