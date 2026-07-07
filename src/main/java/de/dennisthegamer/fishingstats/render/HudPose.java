package de.dennisthegamer.fishingstats.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix3x2fStack;

import java.lang.reflect.Method;

/**
 * Applies the HUD scale across the whole 1.21-1.21.8 range: DrawContext.getMatrices()
 * returns MatrixStack up to 1.21.5 and Matrix3x2fStack from 1.21.6 on. The changed
 * return type makes it a different method at the bytecode level, so a direct call
 * compiled against one version crashes on the other - the lookup happens reflectively
 * instead (found by shape: the only no-arg DrawContext method returning either stack).
 */
final class HudPose {

    private static Method getMatrices;
    private static boolean lookupFailed;

    private HudPose() {}

    /** Pushes a scaled pose; returns null (= render unscaled) if the lookup failed. */
    static Object push(DrawContext graphics, float scale) {
        Object matrices = matrices(graphics);
        if (matrices instanceof Matrix3x2fStack stack) {
            stack.pushMatrix();
            stack.scale(scale, scale);
        } else if (matrices instanceof MatrixStack stack) {
            stack.push();
            stack.scale(scale, scale, 1.0f);
        }
        return matrices;
    }

    static void pop(Object matrices) {
        if (matrices instanceof Matrix3x2fStack stack) {
            stack.popMatrix();
        } else if (matrices instanceof MatrixStack stack) {
            stack.pop();
        }
    }

    private static Object matrices(DrawContext graphics) {
        if (getMatrices == null && !lookupFailed) {
            for (Method method : DrawContext.class.getMethods()) {
                if (method.getParameterCount() == 0
                        && (method.getReturnType() == Matrix3x2fStack.class
                        || method.getReturnType() == MatrixStack.class)) {
                    getMatrices = method;
                    break;
                }
            }
            lookupFailed = getMatrices == null;
        }
        if (getMatrices == null) return null;
        try {
            return getMatrices.invoke(graphics);
        } catch (ReflectiveOperationException e) {
            lookupFailed = true;
            getMatrices = null;
            return null;
        }
    }
}
