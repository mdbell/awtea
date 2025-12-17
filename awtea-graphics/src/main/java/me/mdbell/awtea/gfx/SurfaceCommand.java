package me.mdbell.awtea.gfx;

import lombok.AllArgsConstructor;
import me.mdbell.awtea.gfx.generated.Operation;

public class SurfaceCommand {

    public Operation type;
    public Object obj;
    public int[] args;

    public SurfaceCommand() {
        this.type = Operation.NO_OP;
        this.obj = null;
        this.args = new int[0];
    }

    public SurfaceCommand(Operation operation) {
        this.type = operation;
        this.obj = null;
        this.args = new int[0];
    }

    public SurfaceCommand(Operation operation, Object obj) {
        this.type = operation;
        this.obj = obj;
        this.args = new int[0];
    }

    public SurfaceCommand(Operation operation, Object obj, int... args) {
        this.type = operation;
        this.obj = obj;
        this.args = args;
    }

    public SurfaceCommand(Operation operation, int... args) {
        this.type = operation;
        this.obj = null;
        this.args = args;
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
        this.args = new int[0];
    }

    /**
     * Configures this command with new values, supporting object reuse from a pool.
     * Uses varargs for flexible argument count.
     * 
     * @param operation the operation type
     * @param obj the object parameter (can be null for operations without objects)
     * @param args variable number of integer arguments
     */
    public void configure(Operation operation, Object obj, int... args) {
        this.type = operation;
        this.obj = obj;
        this.args = args;
    }

    /**
     * Configures this command - convenience overload for operations with no parameters.
     */
    public void configure(Operation operation) {
        this.type = operation;
        this.obj = null;
        this.args = new int[0];
    }

    /**
     * Configures this command - convenience overload for operations with object only.
     */
    public void configure(Operation operation, Object obj) {
        this.type = operation;
        this.obj = obj;
        this.args = new int[0];
    }

    @AllArgsConstructor
    public static class PolygonPoints {
        public int[] xpoints;
        public int[] ypoints;
    }
}
