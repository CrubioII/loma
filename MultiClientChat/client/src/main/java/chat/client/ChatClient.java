package chat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.stage.Stage;
import java.net.Socket;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import chat.client.views.LoginView;
import chat.common.model.Message;
import chat.common.model.User;
import chat.common.model.CallSignal;
import chat.client.views.ChatView;

/**
 * Cliente JavaFX para el sistema de chat multiusuario.
 * 
 * Se encarga de:
 * - Mostrar las vistas de login y chat.
 * - Establecer la conexión con el servidor.
 * - Enviar y recibir mensajes de texto, voz y señales de llamada.
 */

public class ChatClient extends Application {
    private ViewManager viewManager;
    private User user;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    @Override
    public void start(Stage primaryStage) {
        viewManager = new ViewManager(primaryStage);
        primaryStage.setTitle("Chat Multiusuario");
        showLoginView();
    }

    private void showLoginView() {
        LoginView loginView = new LoginView(userObj -> {
            this.user = userObj;
            showChatView();
        });
        viewManager.showLoginView(loginView);
    }

    /**
     * Muestra la vista principal del chat.
     */
    private void showChatView() {

        try {
            socket = new Socket("localhost", 12345);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            out.writeObject(user);
            out.flush();
        } catch (Exception ex) {
            showAlertAndReturnToLogin("No se pudo conectar al servidor: " + ex.getMessage());
            return;
        }

        try {
            Object resp = in.readObject();
            if (resp instanceof String && ((String) resp).startsWith("ERROR:USERNAME_TAKEN")) {
                showAlertAndReturnToLogin("Usuario ya conectado en otra instancia.");
                socket.close();
                return;
            }
           
        } catch (Exception ex) {
            showAlertAndReturnToLogin("No se pudo autenticar la sesión: " + ex.getMessage());
            return;
        }

        ChatView chatView = new ChatView(
            user, socket, out, in,
            (to, payload) -> {
                try {
                    out.writeObject(payload);
                    out.flush();
                } catch (Exception ex) {
                    showAlertAndReturnToLogin("Error al enviar mensaje: " + ex.getMessage());
                }
            }
        );
        viewManager.showChatView(chatView);

    
        new Thread(() -> {
            try {
                System.out.println("[DEBUG] Hilo receptor de mensajes iniciado");
                while (true) {
                    Object obj = in.readObject();
                    if (obj instanceof Message) {
                        Message msg = (Message) obj;
                        Platform.runLater(() -> {
                            chatView.receiveMessage(msg);
                        });
                    } else if (obj instanceof CallSignal) {
                        System.out.println("[CALL] Llamada recibida: " + obj);
                        CallSignal signal = (CallSignal) obj;
                        Platform.runLater(() -> {
                            chatView.receiveCallSignal(signal);
                        });
                    } else {
                        System.out.println("[DEBUG] Objeto recibido no es Message ni CallSignal: " + obj);
                    }
                }
            } catch (Exception e) {
                System.out.println("[ERROR] Hilo receptor finalizado: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void showAlertAndReturnToLogin(String msg) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error de conexión");
            alert.setHeaderText(null);
            alert.setContentText(msg);
            alert.showAndWait();
            showLoginView();
        });
    }

    /**
     * Punto de entrada principal del cliente.
     * <p>
     * Lanza la aplicación JavaFX y gestiona la inicialización del cliente.
     * @param args Argumentos de línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        launch(args);
    }
}

