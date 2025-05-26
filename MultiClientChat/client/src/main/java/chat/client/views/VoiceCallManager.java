package chat.client.views;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * VoiceCallManager gestiona llamadas de voz en tiempo real utilizando UDP.
 *
 * Crea y administra dos hilos paralelos: uno para capturar audio desde el micrófono
 * y enviarlo al destinatario, y otro para recibir audio y reproducirlo por los altavoces.
 *
 * Características:
 * - Comunicación full-duplex por UDP entre dos pares.
 * - Transmisión con formato de audio PCM: 16 kHz, 16 bits, mono.
 * - Uso de threads dedicados para envío y recepción.
 *
 * Uso típico:
 * - Crear una instancia con IP/puerto remoto y puerto local.
 * - Llamar a `start()` para comenzar la llamada.
 * - Llamar a `stop()` para finalizar la llamada y cerrar recursos.
 *
 * Ejemplo:
 *   VoiceCallManager call = new VoiceCallManager(remoteIP, remotePort, localPort);
 *   call.start();
 *   ...
 *   call.stop();
 */

public class VoiceCallManager {

    private static final float SAMPLE_RATE = 16000.0f;
    private static final int SAMPLE_SIZE = 16; 
    private static final int CHANNELS = 1;
    private static final int BUFFER_SIZE = 2048; 
    private static final int PACKET_SIZE = 2048; 

    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private DatagramSocket udpSocket;
    private Thread sendThread;
    private Thread receiveThread;
    private AtomicBoolean running = new AtomicBoolean(false);

    private final InetAddress remoteAddress;
    private final int remotePort;
    private final int localPort;

    /**
     * Crea un gestor de llamada de voz UDP.
     * @param remoteAddress IP destino (usuario o servidor relay)
     * @param remotePort Puerto UDP destino
     * @param localPort Puerto UDP local para recibir (puede ser igual/remoto o aleatorio)
     */
    public VoiceCallManager(InetAddress remoteAddress, int remotePort, int localPort) {
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.localPort = localPort;
    }

    /** Inicia la transmisión y recepción de audio. */
    public void start() throws Exception {
        running.set(true);
  
        AudioFormat format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, true, false);
        DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
        microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
        microphone.open(format);
        microphone.start();

        DataLine.Info spkInfo = new DataLine.Info(SourceDataLine.class, format);
        speakers = (SourceDataLine) AudioSystem.getLine(spkInfo);
        speakers.open(format);
        speakers.start();

        udpSocket = new DatagramSocket(localPort);

        sendThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (running.get()) {
                int bytesRead = microphone.read(buffer, 0, buffer.length);
                if (bytesRead > 0) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, bytesRead, remoteAddress, remotePort);
                        udpSocket.send(packet);
                    } catch (IOException e) {
                        System.err.println("[VoiceCall] Error enviando paquete UDP: " + e.getMessage());
                    }
                }
            }
        }, "VoiceCall-SendThread");
        sendThread.start();

        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[PACKET_SIZE];
            while (running.get()) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(packet);

                    speakers.write(packet.getData(), 0, packet.getLength());
                } catch (IOException e) {
                    if (running.get())
                        System.err.println("[VoiceCall] Error recibiendo paquete UDP: " + e.getMessage());
                }
            }
        }, "VoiceCall-ReceiveThread");
        receiveThread.start();
    }

    public void stop() {
        running.set(false);
        if (microphone != null) {
            microphone.stop();
            microphone.close();
        }
        if (speakers != null) {
            speakers.stop();
            speakers.close();
        }
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
        try {
            if (sendThread != null) sendThread.join();
            if (receiveThread != null) receiveThread.join();
        } catch (InterruptedException e) {
        }
    }

    /**
     * Devuelve el puerto UDP local en uso (para negociación con el servidor/otros clientes).
     */
    public int getLocalPort() {
        return (udpSocket != null) ? udpSocket.getLocalPort() : localPort;
    }
}
