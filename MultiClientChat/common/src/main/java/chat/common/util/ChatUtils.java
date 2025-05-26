package chat.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class ChatUtils {
    /**
     * Ensure the user directory, history subfolder, and user.json exist for a given user.
     * Returns the safe username (with hash) used as the folder name.
     */
    public static String ensureUserExists(String displayName, String userdataDir) {
        String safeName = getSafeName(displayName);
        String safeDir = userdataDir + "/" + safeName;
        java.io.File userDir = new java.io.File(safeDir);
        if (!userDir.exists()) userDir.mkdirs();
        java.io.File historyDir = new java.io.File(safeDir + "/history");
        if (!historyDir.exists()) historyDir.mkdirs();
        java.io.File userJson = new java.io.File(safeDir + "/user.json");
        if (!userJson.exists()) {
            chat.common.model.User user = new chat.common.model.User(safeName, displayName);
            try {
                chat.common.util.JSONUtil.writeToJsonFile(user, userJson.getPath());
            } catch (Exception e) {
                System.err.println("[ERROR] Failed to create user.json for " + displayName + ": " + e);
            }
        }
        return safeName;
    }
    /**
     * Devuelve el nombre base antes del '__' en el nombre de directorio o archivo.
     */
    public static String getBaseName(String dirName) {
        int idx = dirName.indexOf("__");
        return idx >= 0 ? dirName.substring(0, idx) : dirName;
    }

    /**
     * Genera el nombre seguro con hash SHA-1.
     */
    public static String getSafeName(String name) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(name.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 3; i++) sb.append(String.format("%02x", hash[i]));
            return name + "__" + sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
