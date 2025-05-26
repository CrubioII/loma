package chat.common.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * RuntimeTypeAdapterFactory para Gson que permite la serialización y deserialización polimórfica.
 * <p>
 * Permite que Gson serialice y deserialice correctamente jerarquías de clases (herencia),
 * agregando un campo de tipo (por ejemplo, "type") en el JSON para identificar la subclase concreta.
 * <p>
 * Uso típico:
 * <pre>
 *     RuntimeTypeAdapterFactory.of(BaseType.class, "type")
 *         .registerSubtype(SubTypeA.class, "subtypeA")
 *         .registerSubtype(SubTypeB.class, "subtypeB");
 * </pre>
 * Después, registrar la fábrica en el GsonBuilder:
 * <pre>
 *     Gson gson = new GsonBuilder()
 *         .registerTypeAdapterFactory(RuntimeTypeAdapterFactory.of(...))
 *         .create();
 * </pre>
 * Esto permite deserializar JSONs polimórficos sin perder el tipo concreto.
 *
 * @param <T> Tipo base de la jerarquía de clases
 */
public class RuntimeTypeAdapterFactory<T> implements TypeAdapterFactory {
    private final Class<?> baseType;
    private final String typeFieldName;
    private final Map<String, Class<?>> labelToSubtype = new HashMap<>();
    private final Map<Class<?>, String> subtypeToLabel = new HashMap<>();

    /**
     * Constructor privado. Utilice el método estático of().
     * @param baseType Clase base de la jerarquía
     * @param typeFieldName Nombre del campo tipo en el JSON
     */
    private RuntimeTypeAdapterFactory(Class<?> baseType, String typeFieldName) {
        this.baseType = baseType;
        this.typeFieldName = typeFieldName;
    }

    /**
     * Crea una nueva instancia de RuntimeTypeAdapterFactory para la clase base dada.
     * @param baseType Clase base
     * @param typeFieldName Nombre del campo tipo en el JSON
     * @param <T> Tipo base
     * @return Instancia de RuntimeTypeAdapterFactory
     */
    public static <T> RuntimeTypeAdapterFactory<T> of(Class<T> baseType, String typeFieldName) {
        return new RuntimeTypeAdapterFactory<>(baseType, typeFieldName);
    }

    /**
     * Registra una subclase y su etiqueta asociada para serialización/deserialización.
     * @param type Clase de la subclase
     * @param label Etiqueta que representa la subclase en el JSON
     * @return Esta instancia para encadenar llamadas
     */
    public RuntimeTypeAdapterFactory<T> registerSubtype(Class<? extends T> type, String label) {
        labelToSubtype.put(label, type);
        subtypeToLabel.put(type, label);
        return this;
    }

    /**
     * Crea el TypeAdapter polimórfico para la jerarquía de clases.
     * Este método es llamado internamente por Gson.
     */
    @Override
    public <R> TypeAdapter<R> create(Gson gson, TypeToken<R> type) {
        if (!baseType.isAssignableFrom(type.getRawType())) {
            return null;
        }
        final Map<String, TypeAdapter<?>> labelToDelegate = new HashMap<>();
        final Map<Class<?>, TypeAdapter<?>> subtypeToDelegate = new HashMap<>();
        for (Map.Entry<String, Class<?>> entry : labelToSubtype.entrySet()) {
            TypeAdapter<?> delegate = gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
            labelToDelegate.put(entry.getKey(), delegate);
            subtypeToDelegate.put(entry.getValue(), delegate);
        }
        return new TypeAdapter<R>() {
            @Override
            public void write(JsonWriter out, R value) throws IOException {
                Class<?> srcType = value.getClass();
                String label = subtypeToLabel.get(srcType);
                if (label == null) {
                    throw new IllegalArgumentException("Cannot serialize " + srcType.getName() + "; did you forget to register a subtype?");
                }
                @SuppressWarnings("unchecked") TypeAdapter<R> delegate = (TypeAdapter<R>) subtypeToDelegate.get(srcType);
                out.beginObject();
                out.name(typeFieldName).value(label);
                // Serializa los campos del objeto como propiedades planas
                JsonObject obj = delegate.toJsonTree(value).getAsJsonObject();
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    out.name(e.getKey());
                    Streams.write(e.getValue(), out);
                }
                out.endObject();
            }
            @Override
            public R read(JsonReader in) throws IOException {
                JsonObject jsonObject = com.google.gson.internal.Streams.parse(in).getAsJsonObject();
                JsonElement typeElement = jsonObject.get(typeFieldName);
                if (typeElement == null) {
                    throw new IOException("Missing type field '" + typeFieldName + "'");
                }
                String typeLabel = typeElement.getAsString();
                @SuppressWarnings("unchecked") TypeAdapter<R> delegate = (TypeAdapter<R>) labelToDelegate.get(typeLabel);
                if (delegate == null) {
                    throw new IOException("Cannot deserialize subtype: " + typeLabel);
                }
                return delegate.fromJsonTree(jsonObject);
            }
        };
    }
}
