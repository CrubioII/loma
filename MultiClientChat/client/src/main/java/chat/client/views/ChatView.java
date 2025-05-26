package chat.client.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;

import java.util.function.BiConsumer;
import chat.common.model.ChatPayload;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sound.sampled.*;

import chat.common.util.ChatUtils;
import chat.common.util.JSONUtil;
import chat.common.util.MessageHistoryUtil;
import chat.client.logic.ChatHistoryManager;
import chat.common.model.AudioFormatWrapper;
import chat.common.model.ChatTarget;
import chat.common.model.Group;
import chat.common.model.Message;
import chat.common.model.CallSignal;
import chat.common.model.User;
import chat.client.model.ChatListItem;

/**
 * Vista principal del cliente de chat.
 *
 * Muestra la lista de chats, mensajes, entrada de texto, botones de llamada y grabación.
 * Soporta mensajes de texto y voz, llamadas UDP y carga de historial.
 *
 * Funciones clave:
 * - receiveMessage: muestra y guarda mensajes entrantes (texto y audio).
 * - receiveCallSignal: gestiona la señalización de llamadas (solicitud, aceptación, rechazo).
 * - loadChatHistory: carga mensajes desde archivo JSON.
 * - stopAudioRecordingAndSend: graba, guarda y envía notas de voz.
 * - refreshChatList: actualiza la vista filtrada de chats.
 *
 * Utiliza JavaFX para la interfaz y serialización para comunicación con el servidor.
 */

public class ChatView extends BorderPane {
    private final String userdataDir;
    {
        String tempDir;
        try {
            String prop = System.getProperty("userdata.dir");
            if (prop != null) {
                tempDir = new java.io.File(prop).getCanonicalPath();
            } else {
                tempDir = new java.io.File("../userdata").getCanonicalPath();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to resolve userdata dir", e);
        }
        userdataDir = tempDir;
        System.out.println("[DEBUG] Using userdata dir: " + userdataDir);
    }
   
    private ListView<ChatListItem> chatListView;
    private ObservableList<ChatListItem> allChats;
    private Button callButton;
    private Button hangupButton;
    private VoiceCallManager callManager;
   
    private Alert waitingAlert;
    private Alert incomingAlert;

    private int localUdpPort = -1;
    private String localUdpHost = null;
    private String peerUdpHost = null;
    private int peerUdpPort = -1;

    
    public void receiveCallSignal(CallSignal signal) {
        Platform.runLater(() -> {
            switch (signal.getType()) {
                case REQUEST:
                    if (signal.getToUser().equals(user.getUsername())) {
                        incomingAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        incomingAlert.setTitle("Llamada entrante");
                        incomingAlert.setHeaderText("Llamada de " + signal.getFrom().getDisplayName());
                        incomingAlert.setContentText("¿Quieres aceptar la llamada?");
                        ButtonType acceptBtn = new ButtonType("Aceptar");
                        ButtonType rejectBtn = new ButtonType("Rechazar");
                        incomingAlert.getButtonTypes().setAll(acceptBtn, rejectBtn);
                        incomingAlert.showAndWait().ifPresent(dialogResult -> {
                            CallSignal response;
                            if (dialogResult == acceptBtn) {
                                try {
                                    java.net.DatagramSocket tempSocket = new java.net.DatagramSocket();
                                    localUdpPort = tempSocket.getLocalPort();
                                    localUdpHost = java.net.InetAddress.getLocalHost().getHostAddress();
                                    tempSocket.close();
                                } catch (Exception e) {
                                    System.err.println("[CALL] Error obteniendo puerto UDP local: " + e.getMessage());
                                    localUdpPort = 0;
                                    localUdpHost = "127.0.0.1";
                                }
                                response = new CallSignal(CallSignal.Type.ACCEPT, user.getUsername(), signal.getFromUser(), null, java.time.LocalDateTime.now(), localUdpHost, localUdpPort);
                                callButton.setDisable(true);
                                hangupButton.setDisable(false);
                            } else {
                                response = new CallSignal(CallSignal.Type.REJECT, user.getUsername(), signal.getFromUser(), null, java.time.LocalDateTime.now());
                                callButton.setDisable(false);
                                hangupButton.setDisable(true);
                            }
                            if (incomingAlert != null) incomingAlert.close();
                            onSendMessage.accept(new User(signal.getFromUser(), signal.getFromUser()), response); 
                        });
                    }
                    break;
                case ACCEPT:
                    if (waitingAlert != null) waitingAlert.close();
                    peerUdpHost = signal.getUdpHost();
                    peerUdpPort = signal.getUdpPort();
                    if (localUdpPort == -1) {
                        try {
                            java.net.DatagramSocket tempSocket = new java.net.DatagramSocket();
                            localUdpPort = tempSocket.getLocalPort();
                            localUdpHost = java.net.InetAddress.getLocalHost().getHostAddress();
                            tempSocket.close();
                        } catch (Exception e) {
                            System.err.println("[CALL] Error obteniendo puerto UDP local: " + e.getMessage());
                            localUdpPort = 0;
                            localUdpHost = "127.0.0.1";
                        }
                        CallSignal acceptResponse = new CallSignal(CallSignal.Type.ACCEPT, user.getUsername(), signal.getFromUser(), null, java.time.LocalDateTime.now(), localUdpHost, localUdpPort);
                        onSendMessage.accept(new User(signal.getFromUser(), signal.getFromUser()), acceptResponse);
                    }
                    if (peerUdpHost != null && peerUdpPort > 0 && localUdpPort > 0) {
                        try {
                            callManager = new VoiceCallManager(java.net.InetAddress.getByName(peerUdpHost), peerUdpPort, localUdpPort);
                            callManager.start();
                            System.out.println("[CALL] Llamada de voz iniciada entre " + localUdpHost + ":" + localUdpPort + " <-> " + peerUdpHost + ":" + peerUdpPort);
                        } catch (Exception e) {
                            System.err.println("[CALL] Error al iniciar llamada de voz: " + e.getMessage());
                        }
                    }
                    Alert accepted = new Alert(Alert.AlertType.INFORMATION, "La llamada fue aceptada. Puedes hablar ahora.", ButtonType.OK);
                    accepted.showAndWait();
                    callButton.setDisable(true);
                    hangupButton.setDisable(false);
                    break;
                case REJECT:
                    if (waitingAlert != null) waitingAlert.close();
                    Alert rejected = new Alert(Alert.AlertType.INFORMATION, "La llamada fue rechazada por el destinatario.", ButtonType.OK);
                    rejected.showAndWait();
                    callButton.setDisable(false);
                    hangupButton.setDisable(true);
                    break;
                case CANCEL:
                    if (waitingAlert != null) waitingAlert.close();
                    if (incomingAlert != null) incomingAlert.close();
                    Alert canceled = new Alert(Alert.AlertType.INFORMATION, "La llamada fue cancelada.", ButtonType.OK);
                    canceled.showAndWait();
                    callButton.setDisable(false);
                    hangupButton.setDisable(true);
                    break;
                case TIMEOUT:
                    if (waitingAlert != null) waitingAlert.close();
                    if (incomingAlert != null) incomingAlert.close();
                    Alert timeout = new Alert(Alert.AlertType.INFORMATION, "La llamada no fue respondida a tiempo y se canceló.", ButtonType.OK);
                    timeout.showAndWait();
                    callButton.setDisable(false);
                    hangupButton.setDisable(true);
                    break;
            }
        });
    }

    public void receiveMessage(Message msg) {
        // Solo mensajes normales (texto/audio)
        if (msg == null || msg.getType() == null) return;
        if (msg.getType().name().startsWith("CALL_")) {
            return;
        }
        String content;
        if (msg.getType() == Message.Type.AUDIO) {
            content = "[Mensaje de voz]";
    
            if (msg.getAudioData() != null && msg.getAudioFilePath() != null) {
                String audioDir = userdataDir + "/" + user.getUsername() + "/";
                String audioPath = audioDir + msg.getAudioFilePath();
                File audioFile = new File(audioPath);
                audioFile.getParentFile().mkdirs();
                if (!audioFile.exists()) { 
                    try (FileOutputStream fos = new FileOutputStream(audioFile)) {
                        fos.write(msg.getAudioData());
                    } catch (Exception e) {
                        System.err.println("[ERROR] No se pudo guardar el archivo de audio recibido: " + e.getMessage());
                    }
                }
                
                try {
                    String filePath = userdataDir + "/" + user.getUsername() + "/history/" + msg.getFrom().getUsername() + ".json";
                    MessageHistoryUtil.saveMessageToHistory(filePath, msg);
                } catch (Exception e) {
                    System.err.println("[ERROR] No se pudo guardar el mensaje de audio en el historial del receptor: " + e.getMessage());
                }
            }
        } else {
            content = msg.getContent();
        }
        messages.add("[" + msg.getTimestamp() + "] " + msg.getFrom().getUsername() + ": " + content);
        messageListView.scrollTo(messages.size() - 1);
        
    }

    private TextField searchField;
    private Label chatTitle;
    private ListView<String> messageListView;
    private ObservableList<String> messages;
    private TextField inputField;
    private Button sendButton;
    private User user;


    private BiConsumer<ChatTarget, ChatPayload> onSendMessage;
    
    private TargetDataLine microphone;
    private ByteArrayOutputStream audioOut;
    private Thread recordingThread;
    private boolean isRecording = false;
    
    
    public ChatView(User user, Socket socket, ObjectOutputStream out, ObjectInputStream in, BiConsumer<ChatTarget, ChatPayload> onSendMessage) {
        this.user = user;
        this.onSendMessage = onSendMessage;

        // --- Botón de llamada de voz ---
        callButton = new Button("📞");
        callButton.setTooltip(new Tooltip("Llamar (voz, UDP)"));
        callButton.setStyle("-fx-background-color: #4f8cff; -fx-text-fill: white; -fx-font-size: 18px; -fx-background-radius: 50%; -fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px; -fx-cursor: hand; margin-left: 8px;");
        callButton.setOnAction(e -> {
            ChatListItem selected = chatListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            ChatTarget toUser = selected.getTarget();
        
            CallSignal callRequest = new CallSignal(CallSignal.Type.REQUEST, user.getUsername(), toUser.getUsername(), null, java.time.LocalDateTime.now());
            onSendMessage.accept(toUser, callRequest);
            callButton.setDisable(true);
            hangupButton.setDisable(false);
            waitingAlert = new Alert(Alert.AlertType.INFORMATION, "Esperando respuesta del destinatario...", ButtonType.OK);
            waitingAlert.show();
         
        });
        hangupButton = new Button("🔚");
        hangupButton.setTooltip(new Tooltip("Colgar llamada"));
        hangupButton.setStyle("-fx-background-color: #ff4f4f; -fx-text-fill: white; -fx-font-size: 18px; -fx-background-radius: 50%; -fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px; -fx-cursor: hand; margin-left: 8px;");
        hangupButton.setDisable(true);
        hangupButton.setOnAction(e -> {
            ChatListItem selected = chatListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;
            ChatTarget toUser = selected.getTarget();
    
            CallSignal cancelSignal = new CallSignal(CallSignal.Type.CANCEL, user.getUsername(), toUser.getUsername(), "Llamada cancelada por el usuario", java.time.LocalDateTime.now());
            onSendMessage.accept(toUser, cancelSignal);

            if (callManager != null) {
                callManager.stop();
                callManager = null;
            }
            hangupButton.setDisable(true);
            callButton.setDisable(false);
            Alert ended = new Alert(Alert.AlertType.INFORMATION, "Llamada finalizada.", ButtonType.OK);
            ended.showAndWait();
        });

        

        Label chatsHeader = new Label("Chats");
        chatsHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 10 0 10 10; -fx-text-fill: #4f8cff;");
        Button addChatButton = new Button("+");
        addChatButton.setStyle("-fx-background-color: #4f8cff; -fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 50%; -fx-min-width: 32px; -fx-min-height: 32px; -fx-max-width: 32px; -fx-max-height: 32px; -fx-cursor: hand; -fx-padding: 0 0 0 0; margin-left: 8px;");
        addChatButton.setTooltip(new Tooltip("Nuevo chat o grupo"));


        HBox callBar = new HBox(8, callButton, hangupButton);
        callBar.setAlignment(Pos.CENTER_LEFT);
        callBar.setPadding(new Insets(6, 0, 6, 0));

        addChatButton.setOnAction(e -> {
            ChoiceDialog<String> typeDialog = new ChoiceDialog<>("Chat individual", "Chat individual", "Grupo");
            typeDialog.setTitle("Nuevo chat o grupo");
            typeDialog.setHeaderText("¿Qué deseas crear?");
            typeDialog.setContentText("Tipo:");
            typeDialog.initOwner(this.getScene() != null ? this.getScene().getWindow() : null);
            typeDialog.showAndWait().ifPresent(type -> {
                if (type.equals("Chat individual")) {
                    TextInputDialog userDialog = new TextInputDialog();
                    userDialog.setTitle("Nuevo chat individual");
                    userDialog.setHeaderText("Nombre de usuario destino:");
                    userDialog.setContentText("Usuario:");
                    userDialog.showAndWait().ifPresent(username -> {
                        if (!username.trim().isEmpty()) {
                        
                            if (username.trim().equalsIgnoreCase(user.getDisplayName()) || username.trim().equalsIgnoreCase(user.getUsername())) {
                                Alert exists = new Alert(Alert.AlertType.WARNING, "No puedes iniciar un chat contigo mismo.", ButtonType.OK);
                                exists.initOwner(this.getScene() != null ? this.getScene().getWindow() : null);
                                exists.showAndWait();
                                return;
                            }
                            String safeName = ChatUtils.ensureUserExists(username.trim(), userdataDir);
                            User dest = new User(safeName, username.trim());
                            if (allChats.stream().noneMatch(c -> c.getUsername().equals(dest.getUsername()))) {
                                ChatListItem item = new ChatListItem(dest);
                                allChats.add(0, item);
                                refreshChatList();
                                chatListView.getSelectionModel().select(item);
                            } else {
                                Alert exists = new Alert(Alert.AlertType.WARNING, "Ya existe un chat con ese usuario.", ButtonType.OK);
                                exists.initOwner(this.getScene() != null ? this.getScene().getWindow() : null);
                                exists.showAndWait();
                            }
                        }
                    });
                } else if (type.equals("Grupo")) {
                    TextInputDialog groupDialog = new TextInputDialog();
                    groupDialog.setTitle("Nuevo grupo");
                    groupDialog.setHeaderText("Nombre del grupo:");
                    groupDialog.setContentText("Nombre:");
                    groupDialog.showAndWait().ifPresent(groupName -> {
                        if (!groupName.trim().isEmpty()) {
                            TextInputDialog membersDialog = new TextInputDialog();
                            membersDialog.setTitle("Miembros del grupo");
                            membersDialog.setHeaderText("Usuarios separados por coma (sin espacios):");
                            membersDialog.setContentText("Miembros:");
                            membersDialog.showAndWait().ifPresent(membersStr -> {
                                Set<User> members = new HashSet<>();
                                String[] names = membersStr.split(",");
                                boolean selfIncluded = false;
                                for (String n : names) {
                                    String trimmed = n.trim();
                                    if (!trimmed.isEmpty()) {
                                 
                                        if (trimmed.equalsIgnoreCase(user.getDisplayName()) || trimmed.equalsIgnoreCase(user.getUsername())) {
                                            selfIncluded = true;
                                            break;
                                        }
                                    }
                                }
                                if (selfIncluded) {
                                    Alert exists = new Alert(Alert.AlertType.WARNING, "No puedes agregarte a ti mismo como miembro del grupo.", ButtonType.OK);
                                    exists.initOwner(this.getScene() != null ? this.getScene().getWindow() : null);
                                    exists.showAndWait();
                                    return;
                                }
                              
                                for (String n : names) {
                                    String trimmed = n.trim();
                                    if (!trimmed.isEmpty()) {
                                        if (trimmed.equalsIgnoreCase(user.getDisplayName()) || trimmed.equalsIgnoreCase(user.getUsername())) {
                                            continue;
                                        }
                                      
                                        String foundSafeName = null;
                                        java.io.File baseDir = new java.io.File("userdata");
                                        java.io.File[] userDirs = baseDir.listFiles(java.io.File::isDirectory);
                                        if (userDirs != null) {
                                            for (java.io.File dir : userDirs) {
                                                java.io.File userJson = new java.io.File(dir, "user.json");
                                                if (userJson.exists()) {
                                                    try {
                                                        chat.common.model.User regUser = chat.common.util.JSONUtil.readFromJsonFile(userJson.getPath(), chat.common.model.User.class);
                                                        if (regUser != null && regUser.getDisplayName().equals(trimmed)) {
                                                            foundSafeName = regUser.getUsername();
                                                            break;
                                                        }
                                                    } catch (Exception ex) { /* ignora error de lectura */ }
                                                }
                                            }
                                        }
                                        if (foundSafeName != null) {
                                            members.add(new User(foundSafeName, trimmed));
                                        } else {
                                           
                                            String safeName = ChatUtils.ensureUserExists(trimmed, userdataDir);
                                            User newUser = new User(safeName, trimmed);
                                            members.add(newUser);
                                        }
                                    }
                                }
                         
                                members.add(user);
                                Group group = new Group(groupName.trim(), groupName.trim(), members);
                                if (allChats.stream().noneMatch(c -> c.getUsername().equals(group.getUsername()))) {
                                    ChatListItem item = new ChatListItem(group);
                                    allChats.add(0, item);
                                    refreshChatList();
                                    chatListView.getSelectionModel().select(item);
                       
                                    try {
                                        String groupDir = userdataDir + "/" + group.getUsername();
                                        File dir = new File(groupDir);
                                        if (!dir.exists()) dir.mkdirs();
                                        JSONUtil.writeToJsonFile(group, groupDir + "/group.json");
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                    // Crear archivo de historial vacío en cada miembro
                                    for (User member : group.getMembers()) {
                                        try {
                                            String memberHistoryDir = userdataDir + "/" + member.getUsername() + "/history";
                                            File histDir = new File(memberHistoryDir);
                                            if (!histDir.exists()) histDir.mkdirs();
                                            File histFile = new File(memberHistoryDir + "/" + group.getUsername() + ".json");
                                            if (!histFile.exists()) histFile.createNewFile();
                                        } catch (Exception ex) {
                                            ex.printStackTrace();
                                        }
                                    }
             
                                    try {
                                        out.writeObject("CREATE_GROUP_OBJ");
                                        out.writeObject(group);
                                        out.flush();
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                } else {
                                    Alert exists = new Alert(Alert.AlertType.WARNING, "Ya existe un grupo con ese nombre.", ButtonType.OK);
                                    exists.initOwner(this.getScene() != null ? this.getScene().getWindow() : null);
                                    exists.showAndWait();
                                    refreshChatList();
                                }
                            });
                        }
                    });
                }
            });
        });
        HBox chatsHeaderBox = new HBox(6, chatsHeader, addChatButton);
        chatsHeaderBox.setAlignment(Pos.CENTER_LEFT);
        chatsHeaderBox.setPadding(new Insets(10, 0, 10, 10));
        
        searchField = new TextField();
        searchField.setPromptText("Buscar chat...");
        searchField.setMinHeight(32);
        searchField.setMaxWidth(Double.MAX_VALUE);
        
        allChats = FXCollections.observableArrayList();
        chatListView = new ListView<>();
        chatListView.setPrefWidth(220);
        chatListView.setStyle("-fx-background-color: transparent; -fx-border-width: 0; -fx-background-insets: 0; -fx-control-inner-background: transparent; -fx-padding: 0; -fx-cell-border-color: transparent;");
        
        File userDir = new File(userdataDir + "/" + user.getUsername() + "/history");
        System.out.println("[DEBUG] userDir: " + userDir);

        // Mostrar miembros del grupo al hacer doble clic en un grupo
        chatListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ChatListItem selected = chatListView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.isGroup()) {
                    Group group = (Group) selected.getTarget();
                    StringBuilder sb = new StringBuilder();
                    sb.append("Miembros del grupo:\n");
                    for (User u : group.getMembers()) {
                        sb.append("- ").append(u.getDisplayName()).append("\n");
                    }
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Miembros del grupo");
                    alert.setHeaderText(group.getDisplayName());
                    alert.setContentText(sb.toString());
                    alert.showAndWait();
                }
            }
        });
        System.out.println(userDir.exists() + " " + userDir.isDirectory());
        if (userDir.exists() && userDir.isDirectory()) {
            String[] chats = userDir.list((dir, name) -> name.endsWith(".json"));
            for (String file : chats) {
                String chatName = file.substring(0, file.length() - 5); // quitar .json
                ChatListItem cli;
                String filePath = userdataDir + "/" + user.getUsername() + "/history/" + chatName + ".json";
                System.out.println("[DEBUG] Trying to load chat history from: " + filePath);
                boolean isAdded = false;
                try {
                    List<Message> history = MessageHistoryUtil.loadHistory(filePath);
                    System.out.println("[DEBUG] Loaded " + history.size() + " messages from " + filePath);
                    if (!history.isEmpty() && history.get(0).getTo() instanceof Group) {
                        Group group = (Group) history.get(0).getTo();
                        if (allChats.stream().noneMatch(c -> c.isGroup() && c.getUsername().equals(group.getUsername()))) {
                            ChatListItem groupItem = new ChatListItem(group);
                            Message last = history.get(history.size() - 1);
                            String preview = last.getType() == Message.Type.AUDIO ? "[Mensaje de voz]" : last.getContent();
                            groupItem.setLastPreview(preview);
                            groupItem.setLastTimestamp(last.getTimestamp());
                            allChats.add(groupItem);
                            isAdded = true;
                        }
                    }
                    if (!isAdded) {
                        cli = new ChatListItem(new User(chatName, chatName));
                        if (!history.isEmpty()) {
                            Message last = history.get(history.size() - 1);
                            String preview = last.getType() == Message.Type.AUDIO ? "[Mensaje de voz]" : last.getContent();
                            cli.setLastPreview(preview);
                            cli.setLastTimestamp(last.getTimestamp());
                        }
                        allChats.add(cli);
                    }
                } catch (Exception ignore) {
                    cli = new ChatListItem(new User(chatName, chatName));
                    allChats.add(cli);
                }
            }
        }
        refreshChatList();
        
       
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            refreshChatList();
        });
        
   
        chatListView.setCellFactory(param -> new ListCell<ChatListItem>() {
            @Override
            protected void updateItem(ChatListItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 0; -fx-border-width: 0;");
                } else {
                    String lastMsg = item.getLastPreview() != null ? item.getLastPreview() : "";
                    String displayName = item.getDisplayName();
                    Label nameLabel = new Label(displayName);
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 15px; -fx-text-fill: #222;");
                    Label previewLabel = new Label(lastMsg);
                    previewLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #888;");
                    VBox vbox = new VBox(nameLabel, previewLabel);
                    vbox.setSpacing(2);
                    setGraphic(vbox);
                    setStyle((isSelected() ? "-fx-background-color: #e5f1ff;" : "-fx-background-color: transparent;") + " -fx-padding: 8 8 8 8; -fx-border-width: 0; -fx-background-radius: 8px; -fx-margin-bottom: 6px;");
                }
            }
        });
        
        VBox leftPanel = new VBox(0, chatsHeaderBox, searchField, chatListView);
        leftPanel.setPrefWidth(220);
        leftPanel.setStyle("-fx-background-color: #f8fafc; -fx-border-width: 0 1 0 0; -fx-border-color: #e0e0e0;");
        VBox.setMargin(searchField, new Insets(0, 10, 10, 10));
        VBox.setMargin(chatListView, new Insets(0, 0, 0, 0));
        

        ChatListItem selectedItem = chatListView.getSelectionModel().getSelectedItem();
        ChatTarget selectedChat = (selectedItem != null) ? selectedItem.getTarget() : null;
        if (selectedChat != null) {
            chatTitle = new Label(selectedChat.getDisplayName());
        } else {
            chatTitle = new Label("Sin chats");
        }
        chatTitle.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #4f8cff; -fx-padding: 8 0 8 12;");
        HBox chatHeader = new HBox(chatTitle);
        chatHeader.setStyle("-fx-background-color: #fff; -fx-border-width: 0 0 1 0; -fx-border-color: #e0e0e0;");
        chatHeader.setMinHeight(48);
        
        messages = FXCollections.observableArrayList();
        messageListView = new ListView<>(messages);
        messageListView.setFocusTraversable(false);
        messageListView.setStyle("-fx-background-color: #f4f6fb; -fx-border-width: 0; -fx-background-insets: 0; -fx-control-inner-background: #f4f6fb; -fx-padding: 0; -fx-cell-border-color: transparent;");
        messageListView.setCellFactory(param -> {
            MessageCell cell = new MessageCell();
            cell.setCurrentUsername(user.getUsername());
            return cell;
        });
        
        inputField = new TextField();
        inputField.setPromptText("Escribe un mensaje...");
        inputField.setPrefWidth(540);
        sendButton = new Button("Enviar");
        sendButton.setPrefWidth(100);
        sendButton.setStyle("-fx-background-color: #4f8cff; -fx-text-fill: white; -fx-font-size: 15px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand;");
        sendButton.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                ChatListItem selectedChatItem = chatListView.getSelectionModel().getSelectedItem();
                if (selectedChatItem == null) {
                    System.err.println("[ADVERTENCIA] No hay chat seleccionado. No se puede enviar el mensaje.");
                    return;
                }
                ChatTarget target = selectedChatItem.getTarget();
                if (target == null) {
                    System.err.println("[ADVERTENCIA] El chat seleccionado no tiene destino válido.");
                    return;
                }
                User toUserObj = null;
                if (target instanceof User) {
                    try {
                        toUserObj = JSONUtil.readFromJsonFile("userdata/" + target.getUsername() + "/user.json", User.class);
                    } catch (Exception ex) {
                        toUserObj = (User) target;
                    }
                } else {
                    toUserObj = (User) target;
                }
                Message msg = new Message(user, toUserObj, Message.Type.TEXT, text, LocalDateTime.now());
                onSendMessage.accept(target, msg);
                
                String now = LocalDateTime.now().toString();
                messages.add("[" + now + "] " + user.getUsername() + ": " + text);
                inputField.clear();
                messageListView.scrollTo(messages.size() - 1);

            } else if (isRecording) {
                stopAudioRecordingAndSend();
            }
        });
        sendButton.setOnMousePressed(e -> {
            if (inputField.getText().trim().isEmpty()) {
                startAudioRecording();
            }
        });
        sendButton.setOnMouseReleased(e -> {
            if (inputField.getText().trim().isEmpty() && isRecording) {
                stopAudioRecordingAndSend();
            }
        });
        inputField.setOnAction(e -> {
            String text = inputField.getText().trim();
            if (!text.isEmpty()) {
                ChatListItem selectedChatItem = chatListView.getSelectionModel().getSelectedItem();
                if (selectedChatItem == null) {
                    System.err.println("[ADVERTENCIA] No hay chat seleccionado. No se puede enviar el mensaje.");
                    return;
                }
                ChatTarget target = selectedChatItem.getTarget();
                if (target == null) {
                    System.err.println("[ADVERTENCIA] El chat seleccionado no tiene destino válido.");
                    return;
                }
                Message msg = new Message(user, target, Message.Type.TEXT, text, LocalDateTime.now());
                onSendMessage.accept(target, msg);
                
                String now = LocalDateTime.now().toString();
                messages.add("[" + now + "] " + user.getUsername() + ": " + text);
                inputField.clear();
                messageListView.scrollTo(messages.size() - 1);
             
            }
        });
        HBox inputBox = new HBox(12, inputField, sendButton);
        inputBox.setPadding(new Insets(10, 18, 10, 18));
        inputBox.setStyle("-fx-background-color: #f8fafc;");
      
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox chatHeaderBar = new HBox(10, chatTitle, spacer, callBar);
        chatHeaderBar.setAlignment(Pos.CENTER_LEFT);
        chatHeaderBar.setPadding(new Insets(10, 16, 10, 16));
        chatHeaderBar.setStyle("-fx-background-color: #eaf2fb; -fx-border-width: 0 0 1 0; -fx-border-color: #d2e3f7;");
        BorderPane chatPane = new BorderPane();
        chatPane.setTop(chatHeaderBar);
        chatPane.setCenter(messageListView);
        chatPane.setBottom(inputBox);
        chatPane.setStyle("-fx-background-color: #f4f6fb;");
        
        this.setLeft(leftPanel);
        this.setCenter(chatPane);
        this.setStyle("-fx-background-color: #f4f6fb;");
        
        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String baseName = ChatUtils.getBaseName(newVal.getUsername());
                chatTitle.setText(baseName);
                System.out.println("[DEBUG] Selected chat: " + newVal.getUsername());
                loadChatHistory(newVal.getUsername());
                System.out.println("[DEBUG] Chat history loaded");
            }
        });
        
        if (!chatListView.getItems().isEmpty()) {
            
            ChatTarget selected = chatListView.getSelectionModel().getSelectedItem().getTarget();
            if (selected != null) {
                chatTitle.setText(ChatUtils.getBaseName(selected.getUsername()));
                System.out.println("[DEBUG] Selected chat: " + selected.getUsername());
                loadChatHistory(selected.getUsername());
                System.out.println("[DEBUG] Chat history loaded");
            }
        }
    }

    public void loadChatHistory(String chatName) {
        messages.clear();
        String filePath = userdataDir + "/" + user.getUsername() + "/history/" + chatName + ".json";
        System.out.println("[DEBUG] Trying to load chat history from: " + filePath);
        java.io.File histFile = new java.io.File(filePath);
        System.out.println("[DEBUG] History file exists: " + histFile.exists());
        try {
            List<Message> history = ChatHistoryManager.loadHistory(filePath);
            if (!history.isEmpty()) {
                for (Message msg : history) {
                    String now = msg.getTimestamp() != null ? msg.getTimestamp().toString() : "";
                    String content;
                    if (msg.getType() == Message.Type.AUDIO) {
                        content = "[Mensaje de voz]";
                    } else {
                        content = msg.getContent();
                    }
                    messages.add("[" + now + "] " + msg.getFrom().getUsername() + ": " + content);
                }
            }
          
        } catch (Exception e) {
            messages.add("[Error al leer historial: " + e.getMessage() + "]");
        }
        messageListView.scrollTo(messages.size() - 1);
    }


    private void startAudioRecording() {
        try {
            AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            audioOut = new ByteArrayOutputStream();
            isRecording = true;
            recordingThread = new Thread(() -> {
                byte[] buffer = new byte[4096];
                while (isRecording) {
                    int count = microphone.read(buffer, 0, buffer.length);
                    if (count > 0) {
                        audioOut.write(buffer, 0, count);
                    }
                }
            });
            recordingThread.start();
        } catch (Exception ex) {
            isRecording = false;
            ex.printStackTrace();
        }
    }

    // Corrige: método para enviar audio aunque el usuario esté offline
    private void stopAudioRecordingAndSend() {
        try {
            isRecording = false;
            microphone.stop();
            microphone.close();
            recordingThread.join();
            byte[] audioBytes = audioOut.toByteArray();
            audioOut.close();
            // Enviar mensaje de voz
            if (audioBytes.length > 1000) { // Solo envía si hay audio real
                ChatListItem selectedChatItem = chatListView.getSelectionModel().getSelectedItem();
                if (selectedChatItem == null) {
                    System.err.println("[ADVERTENCIA] No hay chat seleccionado. No se puede enviar mensaje de audio.");
                    return;
                }
                ChatTarget toUser = selectedChatItem.getTarget();
                if (toUser == null) {
                    System.err.println("[ADVERTENCIA] El chat seleccionado no tiene destino válido para audio.");
                    return;
                }
                System.out.println("[DEBUG] Sending AUDIO message:");
                System.out.println("  From: " + (user != null ? user.getUsername() : "null"));
                System.out.println("  To:   " + (toUser != null ? toUser.getUsername() : "null"));
                if (onSendMessage != null) {
                    // Generar el timestamp UNA SOLA VEZ
                    java.time.LocalDateTime now = java.time.LocalDateTime.now();
                    String timestampStr = now.toString();
                    String safeTimestamp = timestampStr.replaceAll("[:.]", "-");
                    String audioDir = userdataDir + "/" + user.getUsername() + "/history/audio/";
                    File dir = new File(audioDir);
                    if (!dir.exists()) dir.mkdirs();
                    String wavFileName = user.getUsername() + "_" + safeTimestamp + ".wav";
                    String wavPath = audioDir + wavFileName;
                    AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);
                    // Log tamaño y duración estimada
                    System.out.println("[DEBUG] Tamaño buffer PCM: " + audioBytes.length + " bytes");
                    double durationSec = audioBytes.length / (format.getSampleRate() * format.getFrameSize());
                    System.out.println("[DEBUG] Duración estimada: " + durationSec + " segundos");
                    // Guardar buffer PCM crudo para análisis externo
                    String pcmPath = audioDir + user.getUsername() + "_" + safeTimestamp + ".pcm";
                    try (FileOutputStream fos = new FileOutputStream(pcmPath)) {
                        fos.write(audioBytes);
                        System.out.println("[DEBUG] Buffer PCM guardado en: " + pcmPath);
                    } catch (Exception e) {
                        System.err.println("[ERROR] No se pudo guardar el buffer PCM: " + e.getMessage());
                    }
                    // Dump de los primeros bytes del buffer PCM
                    AudioUtils.dumpBytes(audioBytes, 64);
                    AudioUtils.writeWavManual(audioBytes, new File(wavPath), format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels());
                    // Dump de los primeros bytes del archivo WAV generado
                    AudioUtils.dumpFileBytes(new File(wavPath), 64);
                    if (!AudioUtils.isValidWav(new File(wavPath))) {
                        System.err.println("[ERROR] Archivo WAV generado es inválido/corrupto: " + wavPath);
                        return;
                    }
                    // Mensaje para historial (con path relativo)
                    String relativePath = "history/audio/" + wavFileName;
                    // Mensaje para red: incluye los bytes y la ruta
                    Message audioMsg = new Message(
                        user, toUser,
                        Message.Type.AUDIO,
                        "[Audio message]", now, // Usa el mismo timestamp para el mensaje
                        audioBytes,
                        new AudioFormatWrapper(new AudioFormat(16000.0f, 16, 1, true, false))
                    );
                    audioMsg.setAudioFilePath(relativePath); // Para el historial y el receptor
                    onSendMessage.accept(toUser, audioMsg);
                    // Guardar en historial local SOLO UNA VEZ
                    try {
                        String filePath = userdataDir + "/" + user.getUsername() + "/history/" + toUser.getUsername() + ".json";
                        MessageHistoryUtil.saveMessageToHistory(filePath, audioMsg);
                    } catch (Exception e) {
                        System.err.println("[ERROR] No se pudo guardar el mensaje de audio en el historial: " + e.getMessage());
                    }
                }
            }
    
            messages.add("[" + LocalDateTime.now().toString() + "] " + user.getUsername() + ": [Mensaje de voz]");
            messageListView.scrollTo(messages.size() - 1);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private void refreshChatList() {

        ChatListItem selected = chatListView.getSelectionModel().getSelectedItem();
        String selectedUsername = (selected != null) ? selected.getUsername() : null;

        String filter = (searchField != null && searchField.getText() != null) ? searchField.getText().toLowerCase() : "";
        if (filter.isEmpty()) {
            chatListView.setItems(allChats);
        } else {
            chatListView.setItems(allChats.filtered(chat -> chat.getDisplayName().toLowerCase().contains(filter)));
        }


        if (selectedUsername != null) {
            for (ChatListItem item : chatListView.getItems()) {
                if (item.getUsername().equals(selectedUsername)) {
                    chatListView.getSelectionModel().select(item);
                    break;
                }
            }
        }
   
        if (chatListView.getSelectionModel().getSelectedItem() == null && !chatListView.getItems().isEmpty()) {
            chatListView.getSelectionModel().selectFirst();
        }
    }
}