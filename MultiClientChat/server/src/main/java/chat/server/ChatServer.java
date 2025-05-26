package chat.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import chat.common.model.Message;
import chat.common.model.User;
import chat.common.model.Group;
import chat.common.model.CallSignal;
import chat.common.util.MessageHistoryUtil;

/**
 * Servidor de chat multiusuario con soporte para grupos y mensajes de texto/voz.
 * 
 * Gestiona conexiones de clientes, autenticación, envío y reenvío de mensajes,
 * creación de grupos, y almacenamiento de historial. Utiliza TCP y Java Serialization
 * para la comunicación con los clientes.
 * 
 * Responsabilidades principales:
 * 
 *  Gestionar usuarios y grupos conectados.
 *  Recibir, procesar y reenviar mensajes en tiempo real.
 *  Evitar conexiones duplicadas por usuario.
 *  Persistir historial de mensajes y archivos de audio.
 *
 */
public class ChatServer {

    private static final int PORT = 12345;


    private static Map<User, Socket> users = new ConcurrentHashMap<>();
    private static Map<User, ObjectOutputStream> userOutputStreams = new ConcurrentHashMap<>();
    private static Map<String, Group> groups = new ConcurrentHashMap<>(); 

 
    private static final String HISTORY_DIR = System.getProperty("userdata.dir", "userdata") + "/";

    /**
     * Punto de entrada principal del servidor.
     * 
     * Inicia el servidor en el puerto especificado, acepta conexiones de clientes y lanza un hilo por cada cliente conectado.
     * Crea la carpeta de historial si no existe.
     * @param args Argumentos de línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        System.out.println("Servidor de chat iniciado en puerto " + PORT);
        new File(HISTORY_DIR).mkdirs();
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Maneja la conexión y comunicación con un cliente.
     * 
     * Recibe el nombre de usuario, procesa mensajes y comandos recibidos, y gestiona la creación de grupos.
     * @param clientSocket Socket correspondiente al cliente conectado.
     */
    private static final Logger LOGGER = Logger.getLogger(ChatServer.class.getName());

    /**
     * Maneja la conexión y comunicación con un cliente.
     *
     * Recibe el nombre de usuario, procesa mensajes y comandos recibidos, y gestiona la creación de grupos.
     * @param clientSocket Socket correspondiente al cliente conectado.
     */
    private static void handleClient(Socket clientSocket) {
        try (
            ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
            ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream())
        ) {
            System.out.println("[SERVER] Nuevo cliente conectado desde: " + clientSocket.getRemoteSocketAddress());
            User user = (User) in.readObject();
            System.out.println("[SERVER] Usuario recibido: " + user);
            System.out.println(user.getUsername() + " " + user.getDisplayName() + " " + (user.isConnected() ? "online" : "offline"));
        
            users.put(user, clientSocket);
            userOutputStreams.put(user, out);
            out.writeObject("OK");
            out.flush();
            System.out.println("Usuario conectado: " + user.getUsername());
            Object obj;
            System.out.println("[SERVER] Esperando objetos del cliente " + user.getUsername() + "...");
            while ((obj = in.readObject()) != null) {
                LOGGER.info("[SERVER] Objeto recibido: " + obj + " (" + (obj != null ? obj.getClass().getName() : "null") + ")");
                if (obj instanceof Message) {
                    Message msg = (Message) obj;
                    System.out.println("[SERVER] Mensaje recibido: " + msg);
                    if (msg.getType() == Message.Type.TEXT || msg.getType() == Message.Type.AUDIO) {
                        processTextMessage(msg);
                    }
                } else if (obj instanceof CallSignal) {
                    CallSignal signal = (CallSignal) obj;
                    System.out.println("[SERVER] Señal de llamada recibida: " + signal);
                    // Buscar destinatario
                    User toUser = null;
                    for (User u : users.keySet()) {
                        if (u.getUsername().equals(signal.getTo().getUsername())) {
                            toUser = u;
                            break;
                        }
                    }
                    if (toUser != null && userOutputStreams.containsKey(toUser)) {
                        ObjectOutputStream oos = userOutputStreams.get(toUser);
                        try {
                            oos.writeObject(signal);
                            oos.flush();
                            System.out.println("[SERVER] Reenviada señal de llamada a " + toUser.getUsername() + ": " + signal.getType());
                        } catch (Exception ex) {
                            System.out.println("[SERVER] Error reenviando señal de llamada: " + ex.getMessage());
                        }
                    } else {
                        
                        if (signal.getType() == CallSignal.Type.REQUEST) {
                            CallSignal cancelSignal = new CallSignal(
                                CallSignal.Type.CANCEL,
                                signal.getToUser(), 
                                signal.getFromUser(),
                                "El usuario no está disponible para la llamada.",
                                LocalDateTime.now()
                            );
                            ObjectOutputStream oosFrom = userOutputStreams.get(new User(signal.getFromUser(), signal.getFromUser()));
                            if (oosFrom != null) {
                                try {
                                    oosFrom.writeObject(cancelSignal);
                                    oosFrom.flush();
                                } catch (Exception ex) {
                                    System.out.println("[SERVER] Error notificando cancelación al emisor: " + ex.getMessage());
                                }
                            }
                        }
                    }
                } else if (obj instanceof String) {
                    
                    String cmd = (String) obj;
                    if (cmd.startsWith("CREATE_GROUP:")) {
                        String[] parts = cmd.substring(13).split(",");
                        String groupName = parts[0];
                        String displayName = groupName;
                        Set<User> members = new HashSet<>();
                        for (int i = 1; i < parts.length; i++) {
                           
                            String memberUsername = parts[i];
                            User found = null;
                            for (User u : users.keySet()) {
                                if (u.getUsername().equals(memberUsername)) {
                                    found = u;
                                    break;
                                }
                            }
                            if (found != null) {
                                members.add(found);
                            }
                        }
                        Group group = new Group(groupName, displayName, members);
                        groups.put(groupName, group);
                        System.out.println("Grupo creado: " + groupName + " -> " + members);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "[SERVER] Excepción en handleClient", e);
            LOGGER.info("[SERVER] Cliente desconectado o error de conexión: " + e.getMessage());
        } finally {
            LOGGER.info("[SERVER] Hilo de cliente finalizado (cliente desconectado o error)");
       
        try {
            
            User userToRemove = null;
            for (Map.Entry<User, Socket> entry : users.entrySet()) {
                if (entry.getValue().equals(clientSocket)) {
                    userToRemove = entry.getKey();
                    break;
                }
            }
            if (userToRemove != null) {
                users.remove(userToRemove);
                userOutputStreams.remove(userToRemove);
              
                    for (Group group : groups.values()) {
                        if (group.getMembers() != null) {
                            group.getMembers().remove(userToRemove);
                        }
                    }
                }
            } catch (Exception ex) {
                System.out.println("[WARN] Error limpiando recursos de usuario desconectado: " + ex.getMessage());
            }
        }
    }

    /**
     * Devuelve los grupos a los que pertenece un usuario.
     * @param user Usuario a consultar.
     * @return Conjunto de grupos a los que pertenece el usuario.
     */
    public static Set<Group> getGroupsForUser(User user) {
        Set<Group> userGroups = new HashSet<>();
        for (Group group : groups.values()) {
            if (group.getMembers() != null && group.getMembers().contains(user)) {
                userGroups.add(group);
            }
        }
        return userGroups;
    }

    /**
     * Procesa un mensaje de texto recibido.
     * 
     * Guarda el mensaje en el historial y lo reenvía a su destinatario (usuario o grupo).
     * @param msg Mensaje de texto a procesar.
     */
    private static void processTextMessage(Message msg) {
        System.out.println("[DEBUG] Procesando mensaje: " + msg);

        saveHistory(msg);
        System.out.println(msg.getFrom() + " -> " + msg.getTo() + " : " + msg.getContent());
        System.out.println(users.toString());

        if (groups.containsKey(msg.getTo().getUsername())) {

            Group group = groups.get(msg.getTo().getUsername());
            if (group != null && group.getMembers() != null) {
                for (User member : group.getMembers()) {
                    if (users.containsKey(member) && !member.getUsername().equals(msg.getFrom().getUsername())) {
                        System.out.println("[DEBUG] Reenviando a miembro de grupo: " + member.getUsername());
                        sendMessage(users.get(member), msg);
                    }
                }
            }
        } else {
    
            if (users.containsKey(msg.getTo())) {
                System.out.println("[DEBUG] Reenviando a usuario: " + msg.getTo());
                sendMessage(users.get(msg.getTo()), msg);
            }
        }
    }

    /**
     * Envía un mensaje a un usuario a través de su socket.
     * @param socket Socket del usuario destinatario.
     * @param msg Mensaje a enviar.
     */
    private static void sendMessage(Socket socket, Message msg) {
        try {
           
            User user = null;
            for (Map.Entry<User, Socket> entry : users.entrySet()) {
                if (entry.getValue().equals(socket)) {
                    user = entry.getKey();
                    break;
                }
            }
            System.out.println(user);
            if (user != null && userOutputStreams.containsKey(user)) {
                ObjectOutputStream out = userOutputStreams.get(user);
                System.out.println("[DEBUG] Objeto enviado al usuario: " + msg.getClass().getName());
                System.out.println("[DEBUG] Enviando mensaje a usuario " + user.getUsername() + " (" + socket.getRemoteSocketAddress() + "): " + msg);
                out.writeObject(msg);
                out.flush();
            } else {
                System.out.println("[ERROR] No se encontró OOS para el usuario del socket " + socket.getRemoteSocketAddress());
            }
        } catch (Exception e) {
            System.out.println("No se pudo enviar mensaje: " + e.getMessage());
        }
    }

    /**
     * Guarda un mensaje en el historial correspondiente (usuario o grupo).
     * @param msg Mensaje a guardar en el historial.
     */
    private static void saveHistory(Message msg) {
        System.out.println("[DEBUG] Guardando historial para: " + msg.getFrom() + " -> " + msg.getTo());
        
        boolean isAudio = msg.getType() == Message.Type.AUDIO && msg.getAudioData() != null;
        String audioFileName = null;
        String audioFilePath = msg.getAudioFilePath();
        if (isAudio) {
           
            if (audioFilePath == null || audioFilePath.isEmpty()) {
                audioFileName = String.format("%s_%s_%s.wav",
                        msg.getFrom().getUsername(),
                        msg.getTo().getUsername(),
                        msg.getTimestamp().toString().replaceAll("[:.T-]", ""));
                audioFilePath = "history/audio/" + audioFileName;
                msg.setAudioFilePath(audioFilePath);
            } else {
             
                int idx = audioFilePath.lastIndexOf('/');
                audioFileName = idx >= 0 ? audioFilePath.substring(idx + 1) : audioFilePath;
            }
        }
        
        if (msg.getTo() == null || msg.getTo().getUsername() == null) {
            System.out.println("[ERROR] Mensaje recibido con destinatario nulo: " + msg);
            return;
        }
        if (groups != null && groups.containsKey(msg.getTo().getUsername())) {
    try {
        String groupDir = "userdata/" + msg.getTo().getUsername() + "/history";
        new File(groupDir).mkdirs();
        String groupHistoryFile = groupDir + "/" + msg.getTo().getUsername() + ".json";
        MessageHistoryUtil.saveMessageToHistory(groupHistoryFile, msg);
       
        Group groupObj = groups.get(msg.getTo().getUsername());
        if (groupObj != null && groupObj.getMembers() != null) {
            for (User member : groupObj.getMembers()) {
    String memberHistoryDir = "userdata/" + member.getUsername() + "/history";
    new File(memberHistoryDir).mkdirs();
    String memberHistoryFile = memberHistoryDir + "/" + groupObj.getUsername() + ".json";
    System.out.println("[DEBUG][GRUPO] Guardando mensaje en historial de miembro: " + member.getUsername() + " -> " + memberHistoryFile);
    try {
        MessageHistoryUtil.saveMessageToHistory(memberHistoryFile, msg);
        System.out.println("[DEBUG][GRUPO] Guardado exitoso para " + member.getUsername());
    } catch (Exception e) {
        System.out.println("[ERROR][GRUPO] No se pudo guardar historial para miembro " + member.getUsername() + ": " + e.getMessage());
    }
}
        }
        
        if (isAudio && msg.getAudioData() != null) {
            String audioFileNameSafe = String.format("audio_%s_%s.dat", msg.getTimestamp().toString().replaceAll("[:.T-]", ""), msg.getFrom().getUsername());
            File groupAudioDir = new File(groupDir);
            groupAudioDir.mkdirs();
            try (FileOutputStream audioOut = new FileOutputStream(new File(groupAudioDir, audioFileNameSafe))) {
                audioOut.write(msg.getAudioData());
            }
        }
    } catch (Exception e) {
        System.out.println("No se pudo guardar historial JSON de grupo: " + e.getMessage());
    }
    return;
}
        
        System.out.println("[DEBUG] Guardando historial para: " + msg.getFrom() + " -> " + msg.getTo());
        String fromDir = HISTORY_DIR + msg.getFrom().getUsername() + "/history";
        String toDir = HISTORY_DIR + msg.getTo().getUsername() + "/history";
        new File(fromDir).mkdirs();
        new File(toDir).mkdirs();
        String file1 = fromDir + "/" + msg.getTo().getUsername() + ".json";
        String file2 = toDir + "/" + msg.getFrom().getUsername() + ".json";
        try {
            MessageHistoryUtil.saveMessageToHistory(file1, msg);
            MessageHistoryUtil.saveMessageToHistory(file2, msg);
        } catch (Exception e) {
            System.out.println("No se pudo guardar historial JSON privado: " + e.getMessage());
        }
        if (isAudio) {
            
            File audioDirFrom = new File(fromDir + "/audio/");
            File audioDirTo = new File(toDir + "/audio/");
            audioDirFrom.mkdirs();
            audioDirTo.mkdirs();
            try (FileOutputStream audioOut1 = new FileOutputStream(new File(audioDirFrom, audioFileName))) {
                audioOut1.write(msg.getAudioData());
            } catch (Exception e) {
                System.out.println("No se pudo guardar archivo de audio (emisor): " + e.getMessage());
            }
            try (FileOutputStream audioOut2 = new FileOutputStream(new File(audioDirTo, audioFileName))) {
                audioOut2.write(msg.getAudioData());
            } catch (Exception e) {
                System.out.println("No se pudo guardar archivo de audio (receptor): " + e.getMessage());
            }
        }
    }
}



