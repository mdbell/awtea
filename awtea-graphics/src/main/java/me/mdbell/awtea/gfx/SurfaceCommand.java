package me.mdbell.awtea.gfx;

import lombok.AllArgsConstructor;
import me.mdbell.awtea.gfx.generated.Operation;

public class SurfaceCommand {

    public Operation type;
    public Object obj;
    public int arg1;
    public int arg2;
    public int arg3;
    public int arg4;
    public int arg5;
    public int arg6;

    public SurfaceCommand() {
        this(Operation.NO_OP, null, 0, 0, 0, 0, 0, 0);
    }

    public SurfaceCommand(Operation operation) {
        this(operation, null, 0, 0, 0, 0, 0, 0);
    }

    public SurfaceCommand(Operation operation, Object obj) {
        this(operation, obj, 0, 0, 0, 0, 0, 0);
    }

    public SurfaceCommand(Operation operation, Object obj, int arg1) {
        this(operation, obj, arg1, 0, 0, 0, 0, 0);
    }

    public SurfaceCommand(Operation operation, Object obj, int arg1, int arg2) {
        this(operation, obj, arg1, arg2, 0, 0, 0, 0);
    }

    public SurfaceCommand(Operation operation, Object obj, int arg1, int arg2, int arg3) {
        this(operation, obj, arg1, arg2, arg3, 0, 0, 0);
    }

    public SurfaceCommand(Operation operation, Object obj, int arg1, int arg2, int arg3, int arg4) {
        this(operation, obj, arg1, arg2, arg3, arg4, 0, 0);
    }

    public SurfaceCommand(Operation operation, Object obj, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
        this.type = operation;
        this.obj = obj;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
        this.arg4 = arg4;
        this.arg5 = arg5;
        this.arg6 = arg6;
    }

    public SurfaceCommand(Operation operation, int arg1, int arg2, int arg3, int arg4) {
        this(operation, null, arg1, arg2, arg3, arg4, 0, 0);
    }

    public SurfaceCommand(Operation operation, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
        this(operation, null, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    // Note: Operation enum is now defined in
    // me.mdbell.awtea.gfx.generated.Operation
    // Edit schemas/surface-operation.yaml to modify the enum values

    /**
     * Resets this command to a clean state, clearing all fields.
     * Used when returning a command to the object pool to prevent memory leaks.
     */
    public void reset() {
        this.type = Operation.NO_OP;
        this.obj = null;
        this.arg1 = 0;
        this.arg2 = 0;
        this.arg3 = 0;
        this.arg4 = 0;
        this.arg5 = 0;
        this.arg6 = 0;
    }

    /**
     * Configures this command with new values, supporting object reuse from a pool.
     * This method mirrors the full constructor signature.
     */
    public void configure(Operation operation, Object obj, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
        this.type = operation;
        this.obj = obj;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
        this.arg4 = arg4;
        this.arg5 = arg5;
        this.arg6 = arg6;
    }

    /**
     * Configures this command - convenience overload for operations with no parameters.
     */
    public void configure(Operation operation) {
        configure(operation, null, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Configures this command - convenience overload for operations with object only.
     */
    public void configure(Operation operation, Object obj) {
        configure(operation, obj, 0, 0, 0, 0, 0, 0);
    }

    /**
     * Configures this command - convenience overload for operations with object and 1 int arg.
     */
    public void configure(Operation operation, Object obj, int arg1) {
        configure(operation, obj, arg1, 0, 0, 0, 0, 0);
    }

    /**
     * Configures this command - convenience overload for operations with object and 2 int args.
     */
    public void configure(Operation operation, Object obj, int arg1, int arg2) {
        configure(operation, obj, arg1, arg2, 0, 0, 0, 0);
    }

    /**
     * Configures this command - convenience overload for operations with object and 3 int args.
     */
    public void configure(Operation operation, Object obj, int arg1, int arg2, int arg3) {
        configure(operation, obj, arg1, arg2, arg3, 0, 0, 0);
    }

    /**
     * Configures this command - convenience overload for operations with object and 4 int args.
     */
    public void configure(Operation operation, Object obj, int arg1, int arg2, int arg3, int arg4) {
        configure(operation, obj, arg1, arg2, arg3, arg4, 0, 0);
    }

    /**
     * Configures this command - convenience overload for operations with 4 int args (no object).
     */
    public void configure(Operation operation, int arg1, int arg2, int arg3, int arg4) {
        configure(operation, null, arg1, arg2, arg3, arg4, 0, 0);
    }

    /**
     * Configures this command - convenience overload for operations with 6 int args (no object).
     */
    public void configure(Operation operation, int arg1, int arg2, int arg3, int arg4, int arg5, int arg6) {
        configure(operation, null, arg1, arg2, arg3, arg4, arg5, arg6);
    }

    @AllArgsConstructor
    public static class PolygonPoints {
        public int[] xpoints;
        public int[] ypoints;
    }
}
