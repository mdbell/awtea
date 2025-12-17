package me.mdbell.awtea.gfx;

import lombok.AllArgsConstructor;
import me.mdbell.awtea.gfx.generated.Operation;

public class SurfaceCommand {

    public Operation type;
    public Object obj;
    public int[] args;
    public int argCount; // Tracks the actual number of valid args

    public SurfaceCommand() {
        this.type = Operation.NO_OP;
        this.obj = null;
        this.args = new int[6]; // Pre-allocate common size (most commands use <= 6 args)
        this.argCount = 0;
    }

    public SurfaceCommand(Operation operation) {
        this.type = operation;
        this.obj = null;
        this.args = new int[6];
        this.argCount = 0;
    }

    public SurfaceCommand(Operation operation, Object obj) {
        this.type = operation;
        this.obj = obj;
        this.args = new int[6];
        this.argCount = 0;
    }

    public SurfaceCommand(Operation operation, Object obj, int... args) {
        this.type = operation;
        this.obj = obj;
        this.argCount = args.length;
        if (args.length <= 6) {
            this.args = new int[6];
            System.arraycopy(args, 0, this.args, 0, args.length);
        } else {
            this.args = args;
        }
    }

    public SurfaceCommand(Operation operation, int... args) {
        this.type = operation;
        this.obj = null;
        this.argCount = args.length;
        if (args.length <= 6) {
            this.args = new int[6];
            System.arraycopy(args, 0, this.args, 0, args.length);
        } else {
            this.args = args;
        }
    }

    // Note: Operation enum is now defined in
    // me.mdbell.awtea.gfx.generated.Operation
    // Edit schemas/surface-operation.yaml to modify the enum values

    /**
     * Resets this command to a clean state, clearing all fields.
     * Used when returning a command to the object pool to prevent memory leaks.
     * Note: Keeps the args array allocated to avoid re-allocation on next use.
     */
    public void reset() {
        this.type = Operation.NO_OP;
        this.obj = null;
        this.argCount = 0;
        // Keep args array allocated - just reset count
    }

    /**
     * Configures this command with new values, supporting object reuse from a pool.
     * Uses varargs for flexible argument count.
     * Only allocates a new array if the existing one is too small.
     * 
     * @param operation the operation type
     * @param obj the object parameter (can be null for operations without objects)
     * @param args variable number of integer arguments
     */
    public void configure(Operation operation, Object obj, int... args) {
        this.type = operation;
        this.obj = obj;
        this.argCount = args.length;
        
        // Only resize if needed
        if (args.length > this.args.length) {
            this.args = new int[args.length];
        }
        
        // Copy args into our array
        System.arraycopy(args, 0, this.args, 0, args.length);
    }

    /**
     * Configures this command - convenience overload for operations with no parameters.
     */
    public void configure(Operation operation) {
        this.type = operation;
        this.obj = null;
        this.argCount = 0;
    }

    /**
     * Configures this command - convenience overload for operations with object only.
     */
    public void configure(Operation operation, Object obj) {
        this.type = operation;
        this.obj = obj;
        this.argCount = 0;
    }

    @AllArgsConstructor
    public static class PolygonPoints {
        public int[] xpoints;
        public int[] ypoints;
    }
}
