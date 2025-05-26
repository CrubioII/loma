package chat.common.model;
import java.io.Serializable;

public interface ChatTarget extends Serializable {
    String getUsername();
    String getDisplayName();
    boolean isGroup();
}
