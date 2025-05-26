package chat.common.util;

import chat.common.model.ChatTarget;
import chat.common.model.Group;
import chat.common.model.Message;
import chat.common.model.User;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Utilidad para manejar el historial de mensajes de un chat en un archivo JSON.
 * Un archivo por chat, cada archivo contiene una lista de mensajes.
 */
public class MessageHistoryUtil {
    private static final RuntimeTypeAdapterFactory<ChatTarget> chatTargetAdapterFactory =
        RuntimeTypeAdapterFactory
            .of(ChatTarget.class, "type")
            .registerSubtype(User.class, "user")
            .registerSubtype(Group.class, "group");

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .registerTypeAdapterFactory(chatTargetAdapterFactory)
            .create();


    private static final Type MESSAGE_LIST_TYPE = new TypeToken<List<Message>>(){}.getType();

    public static void saveMessageToHistory(String filePath, Message message) throws IOException {
    List<Message> history = loadHistory(filePath);
    // Crear una copia del mensaje con audioData en null si es un mensaje de audio
    Message messageToSave = message;
    if (message.getType() == Message.Type.AUDIO) {
        messageToSave = new Message(
        message.getFrom(),
        message.getTo(),
        message.getType(),
        message.getContent(),
        message.getTimestamp(),
        message.getAudioFilePath(),
        message.getAudioFormatWrapper()
    );
    }
    history.add(messageToSave);
    try (FileWriter writer = new FileWriter(filePath)) {
        String json = gson.toJson(history);
        System.out.println("[DEBUG] JSON a guardar en historial: " + json);
        writer.write(json);
    }
}

    public static List<Message> loadHistory(String filePath) throws IOException {
        System.out.println("[DEBUG] loadHistory: Trying to read file: " + filePath);
        java.io.File f = new java.io.File(filePath);
        System.out.println("[DEBUG] loadHistory: File exists: " + f.exists() + ", length: " + f.length());
        try (FileReader reader = new FileReader(filePath)) {
            List<Message> history = gson.fromJson(reader, MESSAGE_LIST_TYPE);
            System.out.println("[DEBUG] loadHistory: Read " + (history != null ? history.size() : 0) + " messages");
            return history != null ? history : new ArrayList<>();
        } catch (IOException e) {
            System.out.println("[DEBUG] loadHistory: IOException: " + e.getMessage());
            // Si el archivo no existe, retorna lista vacía
            return new ArrayList<>();
        } catch (Exception e) {
            System.out.println("[DEBUG] loadHistory: Exception (likely JSON parse): " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Utilidad para generar el nombre del archivo de historial por chat
    public static String getHistoryFilePath(String userDir, String chatName) {
        return userDir + "/history/" + chatName + ".json";
    }

    private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }

        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }
    }
}
