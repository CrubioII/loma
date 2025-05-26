package chat.common.model;

import java.time.LocalDateTime;

/**
 * Representa una señal de control utilizada para gestionar llamadas de voz.
 * 
 * Se utiliza para establecer, aceptar, rechazar, cancelar o finalizar llamadas
 * entre usuarios a través de UDP. Contiene información sobre los participantes,
 * el tipo de señal, fecha/hora y, opcionalmente, dirección y puerto UDP.
 */

public class CallSignal implements ChatPayload {
    
    public enum Type {
        REQUEST,   
        ACCEPT,     
        REJECT,   
        CANCEL,   
        TIMEOUT    
    }
    private Type type;
    private String fromUser;
    private String toUser;
    private String udpHost;
    private int udpPort = -1;
    private LocalDateTime timestamp;
    private String content; 

    /**
     * Constructor de señal de llamada.
     * @param type Tipo de señal
     * @param fromUser Usuario emisor
     * @param toUser Usuario receptor
     * @param content Mensaje asociado
     * @param timestamp Fecha y hora
     */
    public CallSignal(Type type, String fromUser, String toUser, String content, LocalDateTime timestamp) {
        this.type = type;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.content = content;
        this.timestamp = timestamp;
    }
    public CallSignal(Type type, String fromUser, String toUser, String content, LocalDateTime timestamp, String udpHost, int udpPort) {
        this(type, fromUser, toUser, content, timestamp);
        this.udpHost = udpHost;
        this.udpPort = udpPort;
    }

    // Getters y setters
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public String getFromUser() { return fromUser; }
    public void setFromUser(String fromUser) { this.fromUser = fromUser; }
    public String getToUser() { return toUser; }
    public void setToUser(String toUser) { this.toUser = toUser; }
    public String getUdpHost() { return udpHost; }
    public void setUdpHost(String udpHost) { this.udpHost = udpHost; }
    public int getUdpPort() { return udpPort; }
    public void setUdpPort(int udpPort) { this.udpPort = udpPort; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    @Override
    public ChatTarget getFrom() {
        return new User(fromUser, fromUser);
    }
    @Override
    public ChatTarget getTo() {
        return new User(toUser, toUser);
    }
}
