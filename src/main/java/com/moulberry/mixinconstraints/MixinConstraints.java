package com.moulberry.mixinconstraints;

import com.moulberry.mixinconstraints.checker.AnnotationChecker;
import com.moulberry.mixinconstraints.util.Abstractions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;

public class MixinConstraints {

    public static final Logger LOGGER = LogManager.getLogger("mixinconstraints");
    public static final boolean VERBOSE = "true".equals(System.getProperty("mixinconstraints.verbose"));

    public static boolean shouldApplyMixin(String mixinClassName) {
        try {
            // Use classNode instead of Class.forName to avoid loading at the wrong time
            ClassNode classNode = MixinService.getService().getBytecodeProvider().getClassNode(mixinClassName);

            if (VERBOSE) {
                LOGGER.info("Checking class-level mixin constraints for {}", mixinClassName);
            }

            if (classNode.visibleAnnotations != null) {
                for (AnnotationNode node : classNode.visibleAnnotations) {
                    if (!AnnotationChecker.checkAnnotationNode(node)) {
                        if (VERBOSE) {
                            LOGGER.warn("Preventing application of mixin {} due to failing constraint", mixinClassName);
                        }
                        return false;
                    }
                }
            }

            return true;
        } catch (ClassNotFoundException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return shouldApplyMixin(mixinClassName);
    }

    public static String getLoaderName() {
        return Abstractions.getLoaderName();
    }

    /**
     * @deprecated use {@link #getLoaderName()} instead.
     */
    @Deprecated
    public static Loader getLoader() {
        switch (Abstractions.getLoaderName()) {
            case "Forge": return Loader.FORGE;
            case "NeoForge": return Loader.NEOFORGE;
            case "Fabric": return Loader.FABRIC;
            case "Puzzle Loader": return Loader.PUZZLE_LOADER;
        };
        return Loader.CUSTOM;
    }

    public enum Loader {
        FORGE, NEOFORGE, FABRIC, PUZZLE_LOADER, CUSTOM;

        @Override
        public String toString() {
            switch (this) {
                case FORGE: {
                    return "Forge";
                }
                case NEOFORGE: {
                    return "NeoForge";
                }
                case FABRIC: {
                    return "Fabric";
                }
                case PUZZLE_LOADER: {
                    return "Puzzle Loader";
                }
            }
            return getLoaderName();
        }
    }
}
