package chat.client.views;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.function.Consumer;

import chat.common.util.ChatUtils;
import chat.common.model.User;
import chat.common.util.JSONUtil;
import java.io.IOException;

/**
 * Vista de inicio de sesión del cliente de chat.
 *
 * Permite al usuario ingresar su nombre de usuario y validar su existencia.
 * Si el usuario no existe, se crea automáticamente un nuevo perfil local.
 *
 * Funcionalidades:
 * - Validación de entrada de nombre de usuario.
 * - Creación automática de usuario si no existe.
 * - Llamada al controlador principal una vez autenticado.
 *
 * Componentes principales:
 * - Campo de texto para ingresar el nombre de usuario.
 * - Botón para iniciar sesión.
 * - Mensajes de error para validaciones.
 *
 * Uso:
 * Se utiliza al inicio de la aplicación para autenticar al usuario antes de mostrar la interfaz de chat.
 */

public class LoginView extends BorderPane {
    private TextField userField;
    private Label errorLabel;

    public LoginView(Consumer<User> onLogin) {
        VBox loginBox = new VBox(15);
        loginBox.setId("loginBox");
        loginBox.setAlignment(Pos.CENTER);
        loginBox.setPadding(new Insets(30));

        Text title = new Text("Bienvenido al Chat");
        title.setId("loginTitle");
        Label userLabel = new Label("Nombre de usuario:");
        userField = new TextField();
        userField.setPromptText("Ingrese su nombre...");
        userField.setMaxWidth(220);
        Button loginButton = new Button("Entrar");
        loginButton.setDefaultButton(true);
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #e74c3c;");

        loginButton.setOnAction(e -> handleLogin(onLogin));
        userField.setOnAction(e -> loginButton.fire());

        loginBox.getChildren().addAll(title, userLabel, userField, loginButton, errorLabel);
        this.setCenter(loginBox);
        this.setStyle("-fx-background-color: #f4f6fb;");
    }

    private void handleLogin(Consumer<User> onLogin) {
        String entered = userField.getText().trim();
        if (entered.isEmpty()) {
            errorLabel.setText("Debe ingresar un nombre de usuario.");
            return;
        }
        String userdataDir;
        try {
            String prop = System.getProperty("userdata.dir");
            if (prop != null) {
                userdataDir = new java.io.File(prop).getCanonicalPath();
            } else {
                userdataDir = new java.io.File("../userdata").getCanonicalPath();
            }
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to resolve userdata dir", e);
        }
        File baseDir = new File(userdataDir);
        File[] userDirs = baseDir.listFiles(File::isDirectory);
        boolean found = false;
        String matchedDir = null;
        User user = null;
        if (userDirs != null) {
            for (File dir : userDirs) {
                if (ChatUtils.getBaseName(dir.getName()).equals(entered)) {
                    found = true;
                    matchedDir = dir.getName();
                    try {
                        user = JSONUtil.readFromJsonFile(userdataDir + "/" + matchedDir + "/user.json", User.class);
                    } catch (IOException e) {
                        errorLabel.setText("Error leyendo datos de usuario");
                        e.printStackTrace();
                        return;
                    }
                    break;
                }
            }
        }
        if (!found) {

            String safeName = ChatUtils.ensureUserExists(entered, userdataDir);
            matchedDir = safeName;
            user = new User(safeName, entered);
        }
        errorLabel.setText("");
       
        onLogin.accept(user);
    }
}

