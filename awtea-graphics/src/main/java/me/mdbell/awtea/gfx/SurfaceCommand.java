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

    @AllArgsConstructor
    public static class PolygonPoints {
        public int[] xpoints;
        public int[] ypoints;
    }
}
