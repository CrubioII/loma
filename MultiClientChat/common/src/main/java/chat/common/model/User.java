package chat.common.model;

/**
 * Representa un usuario dentro del sistema de chat.
 * 
 * Implementa {@link ChatTarget} para ser utilizado como emisor o receptor de mensajes.
 * Incluye identificador único, nombre visible y estado de conexión.
 */
public class User implements ChatTarget {
    private String username;
    private String displayName;
    private transient boolean connected = false;

    public User() {} // Requerido para serialización/deserialización (por ejemplo, con Gson)

    /**
     * Crea un nuevo usuario.
     * 
     * @param username identificador único del usuario
     * @param displayName nombre para mostrar en la interfaz
     */
    public User(String username, String displayName) {
        this.username = username;
        this.displayName = displayName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isGroup() {
        return false;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        return (displayName != null) ? displayName : username;
    }
}
