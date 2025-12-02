package me.mdbell.awtea.util.jso;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

public interface Glyph extends JSObject {

    // if true we'll use minified property names
    // and the generator should _not_ create whitespace
    boolean MINIFY = true;
    String X_PROPERTY = MINIFY ? "a" : "x";
    String Y_PROPERTY = MINIFY ? "b" : "y";
    String WIDTH_PROPERTY = MINIFY ? "c" : "width";
    String HEIGHT_PROPERTY = MINIFY ? "d" : "height";
    String ASCENT_PROPERTY = MINIFY ? "e" : "ascent";
    String DESCENT_PROPERTY = MINIFY ? "f" : "descent";
    String ADVANCE_PROPERTY = MINIFY ? "g" : "advance";

    @JSProperty(X_PROPERTY)
    int getX();

    @JSProperty(Y_PROPERTY)
    int getY();

    @JSProperty(WIDTH_PROPERTY)
    int getWidth();

    @JSProperty(HEIGHT_PROPERTY)
    int getHeight();

    @JSProperty(ASCENT_PROPERTY)
    int getAscent();

    @JSProperty(DESCENT_PROPERTY)
    int getDescent();

    @JSProperty(ADVANCE_PROPERTY)
    int getAdvance();
}
