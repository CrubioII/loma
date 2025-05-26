package chat.common.model;

import java.util.Set;
import java.util.HashSet;

/**
 * Representa un grupo de chat.
 * 
 * Un grupo tiene un nombre único, un nombre para mostrar y una lista de miembros (usuarios).
 * Implementa ChatTarget para poder ser destinatario de mensajes grupales.
 */
public class Group implements ChatTarget {
    private String name;
    private String displayName;
    private Set<User> members;
    public Group() {
        this.members = new HashSet<>();
    }

    /**
     * Constructor con nombre y nombre para mostrar. Inicializa la lista de miembros como un conjunto vacío.
     * @param name Nombre único del grupo
     * @param displayName Nombre para mostrar
     */
    public Group(String name, String displayName) {
        this(name, displayName, new HashSet<>());
    }

    /**
     * Constructor de grupo.
     * @param name Nombre único del grupo
     * @param displayName Nombre para mostrar
     * @param members Miembros del grupo
     */
    public Group(String name, String displayName, Set<User> members) {
        this.name = name;
        this.displayName = displayName;
        this.members = members != null ? members : new HashSet<>();
    }

    /**
     * Devuelve la lista de miembros del grupo.
     * @return Miembros del grupo
     */
    public Set<User> getMembers() {
        return members;
    }

    /**
     * Establece la lista de miembros del grupo.
     * @param members Miembros del grupo
     */
    public void setMembers(Set<User> members) {
        this.members = members;
    }
    
    @Override
    /**
     * Devuelve el nombre único del grupo (usado como identificador).
     * @return Nombre del grupo
     */
    public String getUsername() {
        return name;
    }

    @Override
    /**
     * Devuelve el nombre para mostrar del grupo.
     * @return Nombre para mostrar
     */
    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }

    @Override
    public boolean isGroup() {
        return true;
    }
}
