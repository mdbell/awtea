package me.mdbell.awtea.classlib.javax.sound.sampled;

public interface TSourceDataLine extends TDataLine {

    void start();

    int available();

    int write(byte[] b, int off, int len);

}
