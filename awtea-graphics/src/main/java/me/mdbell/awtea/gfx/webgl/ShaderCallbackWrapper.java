package me.mdbell.awtea.gfx.webgl;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Wrapper class that holds a custom shader and its associated rendering callback.
 * This is used internally to pass both objects through the command queue system.
 */
@AllArgsConstructor
@Getter
public class ShaderCallbackWrapper {
    private final CustomShaderProgram shader;
    private final ShaderRenderCallback callback;
}
