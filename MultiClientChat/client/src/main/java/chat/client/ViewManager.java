package chat.client;

import javafx.scene.Scene;
import javafx.stage.Stage;

import chat.client.views.LoginView;
import chat.client.views.ChatView;


/**
 * ViewManager gestiona las escenas de la aplicación cliente.
 * 
 * Permite mostrar y cambiar entre la vista de login y la vista principal de chat.
 */

public class ViewManager {
    private Stage primaryStage;
    private Scene loginScene;
    private Scene chatScene;

    public ViewManager(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

     /**
     * Muestra la vista de login en la ventana principal.
     * @param loginView Vista de login a mostrar
     */
    public void showLoginView(LoginView loginView) {
        if (loginScene == null) {
            loginScene = new Scene(loginView, 400, 300);
            loginScene.getStylesheets().add(getClass().getResource("/ChatClient.css").toExternalForm());
        } else {
            loginScene.setRoot(loginView);
        }
        primaryStage.setScene(loginScene);
        primaryStage.setResizable(false);
        primaryStage.setMinWidth(400);
        primaryStage.setMaxWidth(400);
        primaryStage.setMinHeight(300);
        primaryStage.setMaxHeight(300);
        primaryStage.show();
        primaryStage.centerOnScreen();
    }

     /**
     * Muestra la vista principal del chat.
     * @param chatView Vista de chat a mostrar
     */
    public void showChatView(ChatView chatView) {
        if (chatScene == null) {
            chatScene = new Scene(chatView, 900, 550);
            chatScene.getStylesheets().add(getClass().getResource("/ChatClient.css").toExternalForm());
        } else {
            chatScene.setRoot(chatView);
        }
        primaryStage.setScene(chatScene);
        primaryStage.setResizable(false);
        primaryStage.setMinWidth(900);
        primaryStage.setMaxWidth(900);
        primaryStage.setMinHeight(550);
        primaryStage.setMaxHeight(550);
        primaryStage.centerOnScreen();
    }
}
