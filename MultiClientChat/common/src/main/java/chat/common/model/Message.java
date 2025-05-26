package chat.common.model;

import java.time.LocalDateTime;

/**
 * Representa un mensaje intercambiado entre usuarios en el sistema de chat.
 * 
 * Puede ser un mensaje de texto o de audio. Además de la información básica
 * (remitente, destinatario, contenido, timestamp), puede incluir datos binarios
 * y metadatos asociados al audio.
 */
public class Message implements ChatPayload {

    /**
     * Tipos de mensaje soportados.
     */
    public enum Type {
        TEXT,
        AUDIO
    }

    private ChatTarget from;
    private ChatTarget to;
    private Type type;
    private String content;
    private LocalDateTime timestamp;
    private byte[] audioData;
    private String audioFilePath;
    private AudioFormatWrapper audioFormatWrapper;

    /**
     * Crea un mensaje de texto.
     */
    public Message(ChatTarget from, ChatTarget to, Type type, String content, LocalDateTime timestamp) {
        this.from = from;
        this.to = to;
        this.type = type;
        this.content = content;
        this.timestamp = timestamp;
    }

    /**
     * Crea un mensaje de audio desde archivo.
     */
    public Message(ChatTarget from, ChatTarget to, Type type, String content, LocalDateTime timestamp, String audioFilePath, AudioFormatWrapper audioFormatWrapper) {
        this(from, to, type, content, timestamp);
        this.audioFilePath = audioFilePath;
        this.audioFormatWrapper = audioFormatWrapper;
    }

    /**
     * Crea un mensaje de audio desde memoria (datos en vivo).
     */
    public Message(ChatTarget from, ChatTarget to, Type type, String content, LocalDateTime timestamp, byte[] audioData, AudioFormatWrapper audioFormatWrapper) {
        this(from, to, type, content, timestamp);
        this.audioData = audioData;
        this.audioFormatWrapper = audioFormatWrapper;
    }

    // Getters y setters
    @Override public ChatTarget getFrom() { return from; }
    @Override public ChatTarget getTo() { return to; }
    public Type getType() { return type; }
    public String getContent() { return content; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public byte[] getAudioData() { return audioData; }
    public String getAudioFilePath() { return audioFilePath; }
    public void setAudioFilePath(String audioFilePath) { this.audioFilePath = audioFilePath; }
    public AudioFormatWrapper getAudioFormatWrapper() { return audioFormatWrapper; }
    public void setAudioFormatWrapper(AudioFormatWrapper audioFormatWrapper) { this.audioFormatWrapper = audioFormatWrapper; }
}
