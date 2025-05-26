package chat.client.views;

import javax.sound.sampled.*;
import java.io.*;

public class AudioUtils {
    /**
     * Verifica si un archivo WAV tiene un header válido y no está corrupto.
     * @param wavFile Archivo WAV a verificar
     * @return true si el header es válido
     */
    public static boolean isValidWav(File wavFile) {
        try (FileInputStream fis = new FileInputStream(wavFile)) {
            byte[] header = new byte[44];
            if (fis.read(header) != 44) return false;
            // Revisar el header RIFF/WAVE
            String chunkId = new String(header, 0, 4);
            String format = new String(header, 8, 4);
            String subchunk1Id = new String(header, 12, 4);
            String subchunk2Id = new String(header, 36, 4);
            return chunkId.equals("RIFF") && format.equals("WAVE") && subchunk1Id.equals("fmt ") && subchunk2Id.equals("data");
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Imprime los primeros N bytes de un array en hexadecimal.
     */
    public static void dumpBytes(byte[] data, int n) {
        int len = Math.min(n, data.length);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            sb.append(String.format("%02X ", data[i]));
        }
        System.out.println("[DEBUG] Dump bytes: " + sb.toString());
    }

    /**
     * Imprime los primeros N bytes de un archivo en hexadecimal.
     */
    public static void dumpFileBytes(File file, int n) {
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buf = new byte[n];
            int read = fis.read(buf);
            if (read > 0) dumpBytes(buf, read);
            else System.out.println("[DEBUG] Archivo vacío o no se pudo leer.");
        } catch (IOException e) {
            System.out.println("[DEBUG] Error leyendo archivo: " + e.getMessage());
        }
    }

    /**
     * Escribe un archivo WAV robusto desde datos PCM, verificando el header.
     * @param pcmBytes datos de audio PCM
     * @param format formato de audio
     * @param outFile archivo de salida WAV
     * @throws IOException si hay error de escritura
     */
    public static void writeWav(byte[] pcmBytes, AudioFormat format, File outFile) throws IOException {
        System.out.println("[DEBUG][writeWav] AudioFormat:");
        System.out.println("  Sample Rate:      " + format.getSampleRate());
        System.out.println("  Sample Size Bits: " + format.getSampleSizeInBits());
        System.out.println("  Channels:         " + format.getChannels());
        System.out.println("  Signed:           " + format.getEncoding());
        System.out.println("  Big Endian:       " + format.isBigEndian());
        System.out.println("[DEBUG][writeWav] PCM length: " + pcmBytes.length);
        System.out.println("[DEBUG][writeWav] Frame size: " + format.getFrameSize());
        System.out.println("[DEBUG][writeWav] Frame count: " + (pcmBytes.length / format.getFrameSize()));
        if (pcmBytes.length % format.getFrameSize() != 0) {
            System.err.println("[WARNING][writeWav] PCM length is not a multiple of frame size!");
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(pcmBytes);
             AudioInputStream ais = new AudioInputStream(bais, format, pcmBytes.length / format.getFrameSize())) {
            AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outFile);
            System.out.println("[DEBUG][writeWav] WAV file written: " + outFile.getAbsolutePath());
        } catch (Exception ex) {
            System.err.println("[ERROR][writeWav] Exception while writing WAV: " + ex.getMessage());
            ex.printStackTrace();
            throw ex;
        }
    }

    /**
     * Escribe un archivo WAV estándar manualmente, para máxima compatibilidad.
     * @param pcmBytes datos de audio PCM
     * @param outFile archivo de salida WAV
     * @param sampleRate frecuencia de muestreo (Hz)
     * @param bitsPerSample bits por muestra (ej. 16)
     * @param channels número de canales (1=mono, 2=stereo)
     */
    public static void writeWavManual(byte[] pcmBytes, File outFile, float sampleRate, int bitsPerSample, int channels) throws IOException {
        try (FileOutputStream out = new FileOutputStream(outFile)) {
            int byteRate = (int) (sampleRate * channels * bitsPerSample / 8);
            int blockAlign = channels * bitsPerSample / 8;
            int dataSize = pcmBytes.length;
            int chunkSize = 36 + dataSize;

            // RIFF header
            out.write("RIFF".getBytes());
            out.write(intToLittleEndian(chunkSize));
            out.write("WAVE".getBytes());
            // fmt chunk
            out.write("fmt ".getBytes());
            out.write(intToLittleEndian(16)); // Subchunk1Size for PCM
            out.write(shortToLittleEndian((short) 1)); // AudioFormat PCM
            out.write(shortToLittleEndian((short) channels));
            out.write(intToLittleEndian((int) sampleRate));
            out.write(intToLittleEndian(byteRate));
            out.write(shortToLittleEndian((short) blockAlign));
            out.write(shortToLittleEndian((short) bitsPerSample));
            // data chunk
            out.write("data".getBytes());
            out.write(intToLittleEndian(dataSize));
            // PCM data
            out.write(pcmBytes);
            System.out.println("[DEBUG][writeWavManual] WAV file written: " + outFile.getAbsolutePath());
        }
    }

    private static byte[] intToLittleEndian(int value) {
        return new byte[] {
            (byte) (value & 0xff),
            (byte) ((value >> 8) & 0xff),
            (byte) ((value >> 16) & 0xff),
            (byte) ((value >> 24) & 0xff)
        };
    }

    private static byte[] shortToLittleEndian(short value) {
        return new byte[] {
            (byte) (value & 0xff),
            (byte) ((value >> 8) & 0xff)
        };
    }
}

