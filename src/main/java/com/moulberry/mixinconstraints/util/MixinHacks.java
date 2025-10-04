package com.moulberry.mixinconstraints.util;

import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.ext.Extensions;
import org.spongepowered.asm.mixin.transformer.ext.IExtension;
import org.spongepowered.asm.mixin.transformer.ext.ITargetClassContext;
import org.spongepowered.asm.mixin.transformer.ext.extensions.ExtensionCheckClass;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.SortedSet;

/**
 * Most of this class is adapted from MixinExtras's {@code MixinInternals} class, by LlamaLad7.
 * MixinExtras is licensed under the MIT License: <a href="https://github.com/LlamaLad7/MixinExtras/blob/master/LICENSE">here</a>.
 */
@SuppressWarnings("unchecked")
public final class MixinHacks {
    private static MethodHandle TARGET_CLASS_CONTEXT_MIXINS;
    private static MethodHandle MIXIN_INFO_GET_STATE;
    private static MethodHandle STATE_CLASS_NODE;

    private static MethodHandle EXTENSIONS_EXTENSIONS;
    private static MethodHandle EXTENSIONS_ACTIVE_EXTENSIONS_GET;
    private static MethodHandle EXTENSIONS_ACTIVE_EXTENSIONS_SET;

    private static boolean initialized = false;


    private static void tryInit() {
        if (initialized) {
            return;
        }
        initialized = true;

        try {
            Class<?> TargetClassContext = Class.forName("org.spongepowered.asm.mixin.transformer.TargetClassContext");
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            Field field = TargetClassContext.getDeclaredField("mixins");
            field.setAccessible(true);
            TARGET_CLASS_CONTEXT_MIXINS = lookup.unreflectGetter(field);

            Class<?> MixinInfo = Class.forName("org.spongepowered.asm.mixin.transformer.MixinInfo");
            Class<?> MixinInfo$State = Class.forName("org.spongepowered.asm.mixin.transformer.MixinInfo$State");

            Method method = MixinInfo.getDeclaredMethod("getState");
            method.setAccessible(true);
            MIXIN_INFO_GET_STATE = lookup.unreflect(method);

            field = MixinInfo$State.getDeclaredField("classNode");
            field.setAccessible(true);
            STATE_CLASS_NODE = lookup.unreflect(method);

            field = Extensions.class.getDeclaredField("extensions");
            field.setAccessible(true);
            EXTENSIONS_EXTENSIONS = lookup.unreflectGetter(field);
            field = Extensions.class.getDeclaredField("activeExtensions");
            field.setAccessible(true);
            EXTENSIONS_ACTIVE_EXTENSIONS_GET = lookup.unreflectGetter(field);
            EXTENSIONS_ACTIVE_EXTENSIONS_SET = lookup.unreflectSetter(field);

        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static void registerMixinExtension(IExtension extension) {
        tryInit();
        try {
            Extensions extensions = (Extensions) ((IMixinTransformer) MixinEnvironment.getDefaultEnvironment().getActiveTransformer())
                    .getExtensions();
            addExtension((List<IExtension>) EXTENSIONS_EXTENSIONS.invokeExact(extensions), extension);

            List<IExtension> activeExtensions = new ArrayList<>((List<IExtension>) EXTENSIONS_ACTIVE_EXTENSIONS_GET.invokeExact(extensions));
            addExtension(activeExtensions, extension);

            EXTENSIONS_ACTIVE_EXTENSIONS_SET.invokeExact(extensions, Collections.unmodifiableList(activeExtensions));
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public static List<Pair<IMixinInfo, ClassNode>> getMixinsFor(ITargetClassContext context) {
        tryInit();
        List<Pair<IMixinInfo, ClassNode>> result = new ArrayList<>();
        try {
            // note: can't use invokeExact here because TargetClassContext is not public
            for(IMixinInfo mixin : (SortedSet<IMixinInfo>) TARGET_CLASS_CONTEXT_MIXINS.invoke(context)) {
                ClassNode classNode = (ClassNode) STATE_CLASS_NODE.invoke(MIXIN_INFO_GET_STATE.invoke(mixin));
                result.add(Pair.of(mixin, classNode));
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private static void addExtension(List<IExtension> extensions, IExtension newExtension) {
        extensions.add(0, newExtension);

        // If this runs before our extensions it will fail since we're not done generating our bytecode.
        List<IExtension> lateExtensions = new ArrayList<>();
        for (ListIterator<IExtension> it = extensions.listIterator(); it.hasNext(); ) {
            IExtension extension = it.next();
            if (extension instanceof ExtensionCheckClass) {
                it.remove();
                lateExtensions.add(extension);
            }
        }
        extensions.addAll(lateExtensions);
    }

}
