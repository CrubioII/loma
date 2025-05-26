package chat.common.model;

import java.io.Serializable;

/**
 * Interfaz común para cualquier "payload" de chat (mensajes, señales, etc).
 * Permite obtener remitente y destinatario como ChatTarget (User, Group, etc).
 */
public interface ChatPayload extends Serializable {
    ChatTarget getFrom();
    ChatTarget getTo();
}
