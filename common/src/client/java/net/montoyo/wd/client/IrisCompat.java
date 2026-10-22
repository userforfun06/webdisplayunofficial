package net.montoyo.wd.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

/**
 * Optional Iris compatibility. Kept reflection-only so WebDisplays does not
 * require Iris at compile time or runtime.
 */
public final class IrisCompat {
    private static final MethodHandle SHADOW_PASS = findShadowPass();

    private IrisCompat() {
    }

    /**
     * @return true while Iris is rendering its shadow pass
     */
    public static boolean isRenderingShadowPass() {
        MethodHandle shadowPass = SHADOW_PASS;
        if (shadowPass == null)
            return false;

        try {
            return (boolean) shadowPass.invoke();
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static MethodHandle findShadowPass() {
        try {
            Class<?> apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Method method = apiClass.getMethod("isRenderingShadowPass");
            return MethodHandles.lookup().unreflect(method).bindTo(api);
        } catch (Throwable ignored) {
            // Older/custom Iris builds: fall back to the internal state class.
        }

        try {
            Class<?> stateClass = Class.forName("net.irisshaders.iris.shadows.ShadowRenderingState");
            Method method = stateClass.getMethod("areShadowsCurrentlyBeingRendered");
            return MethodHandles.lookup().unreflect(method);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
