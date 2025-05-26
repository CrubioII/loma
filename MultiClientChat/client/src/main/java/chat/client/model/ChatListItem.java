package chat.client.model;

import chat.common.model.ChatTarget;
import java.time.LocalDateTime;

/**
 * Representa un elemento de la lista de chats en la UI del cliente.
 * <p>
 * Contiene información sobre el usuario o grupo, el último mensaje y notificaciones de nuevos mensajes.
 */
public class ChatListItem {
    private final ChatTarget target;
    private String lastPreview;
    private LocalDateTime lastTimestamp;

    public ChatListItem(ChatTarget target) {
        this.target = target;
        this.lastPreview = "";
        this.lastTimestamp = null;
    }

    public ChatTarget getTarget() { return target; }
    public String getLastPreview() { return lastPreview; }
    public void setLastPreview(String lastPreview) { this.lastPreview = lastPreview; }
    public LocalDateTime getLastTimestamp() { return lastTimestamp; }
    public void setLastTimestamp(LocalDateTime lastTimestamp) { this.lastTimestamp = lastTimestamp; }
    public String getUsername() { return target.getUsername(); }
    public String getDisplayName() { return target.getDisplayName(); }
    public boolean isGroup() { return target.isGroup(); }
}
