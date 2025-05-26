package chat.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utilidad para leer y escribir objetos Java como JSON en disco.
 * Puede ser usada tanto por el cliente como el servidor.
 */
public class JSONUtil {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    /**
     * Guarda un objeto como JSON en el archivo especificado.
     */
    public static <T> void writeToJsonFile(T object, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(object, writer);
        }
    }

    /**
     * Lee un objeto de un archivo JSON.
     */
    public static <T> T readFromJsonFile(String filePath, Class<T> clazz) throws IOException {
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, clazz);
        }
    }
}
