package me.mdbell.awtea.classlib.javax.sound.midi;

import lombok.Getter;
import lombok.ToString;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * @see javax.sound.midi.Sequence
 */
@Getter
@ToString
public class TSequence {

    private final byte[] data;

    public TSequence(InputStream in) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        try {
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.data = out.toByteArray();
    }
}
