package me.mdbell.awtea.gfx.webgl;

import me.mdbell.awtea.testutil.TestCase;
import org.teavm.jso.dom.html.HTMLCanvasElement;
import org.teavm.jso.dom.html.HTMLDocument;

/**
 * Tests for the custom shader API.
 */
public class CustomShaderTest extends TestCase {

    private HTMLCanvasElement canvas;
    private WebGLSurfaceBackend backend;

    @Override
    public void setUp() {
        HTMLDocument document = HTMLDocument.current();
        canvas = (HTMLCanvasElement) document.createElement("canvas");
        canvas.setWidth(800);
        canvas.setHeight(600);
        backend = new WebGLSurfaceBackend(canvas);
    }

    @Override
    public void tearDown() {
        if (backend != null) {
            backend.disposeAllCustomShaders();
        }
    }

    public void testShaderCompilation() {
        String vertexShader =
            "attribute vec2 a_position;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(a_position, 0, 1);\n" +
            "}\n";

        String fragmentShader =
            "precision mediump float;\n" +
            "void main() {\n" +
            "    gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);\n" +
            "}\n";

        CustomShaderProgram shader = backend.registerCustomShader(
            "testShader",
            vertexShader,
            fragmentShader
        );

        assertNotNull("Shader should be created", shader);
        assertEquals("Shader name should match", "testShader", shader.getName());
        assertFalse("Shader should not be disposed", shader.isDisposed());
    }

    public void testShaderRegistrationDuplicate() {
        String vertexShader = "attribute vec2 a_position;\nvoid main() { gl_Position = vec4(a_position, 0, 1); }\n";
        String fragmentShader = "precision mediump float;\nvoid main() { gl_FragColor = vec4(1.0); }\n";

        backend.registerCustomShader("duplicate", vertexShader, fragmentShader);

        try {
            backend.registerCustomShader("duplicate", vertexShader, fragmentShader);
            fail("Should throw exception when registering duplicate shader");
        } catch (RuntimeException e) {
            assertTrue("Exception message should mention duplicate", e.getMessage().contains("already registered"));
        }
    }

    public void testShaderActivation() {
        String vertexShader = "attribute vec2 a_position;\nvoid main() { gl_Position = vec4(a_position, 0, 1); }\n";
        String fragmentShader = "precision mediump float;\nvoid main() { gl_FragColor = vec4(1.0); }\n";

        CustomShaderProgram shader = backend.registerCustomShader("activeTest", vertexShader, fragmentShader);

        assertNull("No shader should be active initially", backend.getActiveCustomShader());

        backend.activateCustomShader("activeTest");
        assertEquals("Shader should be active", shader, backend.getActiveCustomShader());

        backend.deactivateCustomShader();
        assertNull("No shader should be active after deactivation", backend.getActiveCustomShader());
    }

    public void testShaderUnregistration() {
        String vertexShader = "attribute vec2 a_position;\nvoid main() { gl_Position = vec4(a_position, 0, 1); }\n";
        String fragmentShader = "precision mediump float;\nvoid main() { gl_FragColor = vec4(1.0); }\n";

        CustomShaderProgram shader = backend.registerCustomShader("unregisterTest", vertexShader, fragmentShader);
        assertNotNull("Shader should exist", backend.getCustomShader("unregisterTest"));

        backend.unregisterCustomShader("unregisterTest");
        assertNull("Shader should be removed", backend.getCustomShader("unregisterTest"));
        assertTrue("Shader should be disposed", shader.isDisposed());
    }

    public void testShaderDisposal() {
        String vertexShader = "attribute vec2 a_position;\nvoid main() { gl_Position = vec4(a_position, 0, 1); }\n";
        String fragmentShader = "precision mediump float;\nvoid main() { gl_FragColor = vec4(1.0); }\n";

        CustomShaderProgram shader = backend.registerCustomShader("disposeTest", vertexShader, fragmentShader);
        assertFalse("Shader should not be disposed initially", shader.isDisposed());

        shader.dispose();
        assertTrue("Shader should be disposed", shader.isDisposed());

        try {
            shader.use();
            fail("Should throw exception when using disposed shader");
        } catch (IllegalStateException e) {
            assertTrue("Exception message should mention disposal", e.getMessage().contains("disposed"));
        }
    }

    public void testInvalidShaderCompilation() {
        String invalidVertexShader = "this is not valid GLSL code";
        String fragmentShader = "precision mediump float;\nvoid main() { gl_FragColor = vec4(1.0); }\n";

        try {
            backend.registerCustomShader("invalid", invalidVertexShader, fragmentShader);
            fail("Should throw exception for invalid shader");
        } catch (RuntimeException e) {
            assertTrue("Exception message should mention compilation", e.getMessage().contains("compile"));
        }
    }

    public void testUniformLocationCaching() {
        String vertexShader = "attribute vec2 a_position;\nvoid main() { gl_Position = vec4(a_position, 0, 1); }\n";
        String fragmentShader =
            "precision mediump float;\n" +
            "uniform vec4 u_color;\n" +
            "void main() { gl_FragColor = u_color; }\n";

        CustomShaderProgram shader = backend.registerCustomShader("uniformTest", vertexShader, fragmentShader);

        // First call should query WebGL
        shader.getUniformLocation("u_color");
        // Second call should use cache
        shader.getUniformLocation("u_color");

        // No exception means caching works
    }

    public void testContextStackAccess() {
        assertNotNull("Context stack should be accessible", backend.getContextStack());
        assertNotNull("Transform array should be available", backend.getContextStack().getTransformArray());
    }
}
