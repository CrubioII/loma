package chat.client.logic;

import chat.common.model.Message;
import chat.common.util.MessageHistoryUtil;
import java.util.List;

/**
 * Gestiona la carga y obtención de historial de mensajes en el cliente.
 * <p>
 * Utiliza utilidades de MessageHistoryUtil para acceder a los historiales
 * almacenados en archivos JSON. Permite obtener el historial completo,
 * el último mensaje y una vista previa del último mensaje para mostrar en la UI.
 */
public class ChatHistoryManager {
    /**
     * Carga el historial de mensajes desde el archivo especificado.
     * @param filePath Ruta al archivo de historial
     * @return Lista de mensajes (puede ser vacía si no existe o hay error)
     */
    public static List<Message> loadHistory(String filePath) {
        try {
            return MessageHistoryUtil.loadHistory(filePath);
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Obtiene el último mensaje del historial del archivo dado.
     * @param filePath Ruta al archivo de historial
     * @return Último mensaje o null si no hay mensajes
     */
    public static Message getLastMessage(String filePath) {
        List<Message> history = loadHistory(filePath);
        if (!history.isEmpty()) {
            return history.get(history.size() - 1);
        }
        return null;
    }

    /**
     * Devuelve una vista previa del último mensaje (texto o marcador de audio).
     * @param last Último mensaje
     * @return Cadena para mostrar como preview
     */
    public static String getLastPreview(Message last) {
        if (last == null) return "";
        return last.getType() == Message.Type.AUDIO ? "[Mensaje de voz]" : last.getContent();
    }
}
