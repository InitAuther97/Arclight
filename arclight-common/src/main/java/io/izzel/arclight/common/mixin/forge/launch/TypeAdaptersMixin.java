package io.izzel.arclight.common.mixin.forge.launch;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Mixin(targets = "com.google.gson.internal.bind.TypeAdapters")
public abstract class TypeAdaptersMixin {

    @Shadow
    @Mutable
    @Final
    public static TypeAdapterFactory ENUM_FACTORY;

    /*@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Class;getField(Ljava/lang/String;)Ljava/lang/reflect/Field;"))
    public Field constructor(Class<?> instance, String name) {
        System.out.println("EnumTypeAdapter constructor");

        Field f;
        try {
            f = instance.getField(name);
        } catch (NoSuchFieldException e) {
            try {
                f = getClass().getDeclaredField("placeholder");
            } catch (NoSuchFieldException ex) {
                throw new RuntimeException("Arclight cannot redirect getField(): placeholder not found", ex);
            }
        }
        return f;
    }*/

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void afterClinit(CallbackInfo ci) {
        class ArclightEnumAdapter<T extends Enum<T>> extends TypeAdapter<T> {
            private final Map<String, T> nameToConstant = new HashMap<>();
            private final Map<T, String> constantToName = new HashMap<>();

            ArclightEnumAdapter(Class<T> classOfT) {
                for (T constant : classOfT.getEnumConstants()) {
                    String name = constant.name();
                    SerializedName annotation;
                    try {
                        annotation = classOfT.getField(name).getAnnotation(SerializedName.class);
                    } catch (NoSuchFieldException e) {
                        annotation = null;
                    }
                    if (annotation != null) {
                        name = annotation.value();
                        for (String alternate : annotation.alternate()) {
                            nameToConstant.put(alternate, constant);
                        }
                    }
                    nameToConstant.put(name, constant);
                    constantToName.put(constant, name);
                }
            }

            @Override
            public T read(JsonReader in) throws IOException {
                if (in.peek() == JsonToken.NULL) {
                    in.nextNull();
                    return null;
                }
                return nameToConstant.get(in.nextString());
            }

            @Override
            public void write(JsonWriter out, T value) throws IOException {
                out.value(value == null ? null : constantToName.get(value));
            }
        }
        ENUM_FACTORY = new TypeAdapterFactory() {
            @Override
            public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
                Class<? super T> rawType = typeToken.getRawType();
                if (!Enum.class.isAssignableFrom(rawType) || rawType == Enum.class) {
                    return null;
                }
                if (!rawType.isEnum()) {
                    rawType = rawType.getSuperclass(); // handle anonymous subclasses
                }
                return (TypeAdapter<T>) new ArclightEnumAdapter(rawType);
            }
        };
    }
}
