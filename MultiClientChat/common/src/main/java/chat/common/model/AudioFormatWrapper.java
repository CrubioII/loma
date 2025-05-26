package chat.common.model;

import java.io.Serializable;

import javax.sound.sampled.AudioFormat;

/**
 * Envuelve un objeto AudioFormat para permitir su serialización.
 * 
 * Esta clase permite transportar información del formato de audio (PCM)
 * a través de la red o almacenarla, ya que AudioFormat no implementa Serializable.
 * 
 * Uso:
 * - Crear un AudioFormatWrapper desde un AudioFormat.
 * - Convertirlo de vuelta con el método toAudioFormat().
 */

public class AudioFormatWrapper implements Serializable {
    private float sampleRate;
    private int sampleSizeInBits;
    private int channels;
    private boolean signed;
    private boolean bigEndian;

    public AudioFormatWrapper() {}

    public AudioFormatWrapper(AudioFormat format) {
        this.sampleRate = format.getSampleRate();
        this.sampleSizeInBits = format.getSampleSizeInBits();
        this.channels = format.getChannels();
        this.signed = format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED;
        this.bigEndian = format.isBigEndian();
    }


    /**
     * Convierte el wrapper nuevamente a un objeto AudioFormat.
     * @return AudioFormat con los valores encapsulados
     */
    public AudioFormat toAudioFormat() {
        return new AudioFormat(
            sampleRate,
            sampleSizeInBits,
            channels,
            signed,
            bigEndian
        );
    }

    // Getters y setters
    public float getSampleRate() { return sampleRate; }
    public void setSampleRate(float sampleRate) { this.sampleRate = sampleRate; }
    public int getSampleSizeInBits() { return sampleSizeInBits; }
    public void setSampleSizeInBits(int sampleSizeInBits) { this.sampleSizeInBits = sampleSizeInBits; }
    public int getChannels() { return channels; }
    public void setChannels(int channels) { this.channels = channels; }
    public boolean isSigned() { return signed; }
    public void setSigned(boolean signed) { this.signed = signed; }
    public boolean isBigEndian() { return bigEndian; }
    public void setBigEndian(boolean bigEndian) { this.bigEndian = bigEndian; }
}

