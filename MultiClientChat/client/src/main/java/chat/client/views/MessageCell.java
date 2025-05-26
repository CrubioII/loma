package chat.client.views;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.File;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.Clip;


/**
 * Celda personalizada para mostrar mensajes de chat en JavaFX.
 *
 * Esta clase personaliza cada celda de la lista de mensajes, mostrando mensajes
 * de texto o etiquetas interactivas para mensajes de voz.
 *
 * Características:
 * - Distingue entre mensajes propios y de otros usuarios.
 * - Aplica estilos distintos para burbujas de mensajes (colores, alineación).
 * - Reproduce mensajes de voz si el contenido corresponde a "[Mensaje de voz]".
 *
 * Requiere que se establezca el nombre de usuario actual para determinar el origen
 * de cada mensaje mediante `setCurrentUsername(String username)`.
 *
 * El mensaje debe seguir el formato:
 *   "[yyyy-MM-ddTHH:mm:ss] usuario: contenido"
 * Donde el contenido puede ser texto plano o el marcador "[Mensaje de voz]".
 *
 * Ejemplo de uso:
 *   messageListView.setCellFactory(param -> {
 *       MessageCell cell = new MessageCell();
 *       cell.setCurrentUsername(usuarioActual);
 *       return cell;
 *   });
 */


public class MessageCell extends ListCell<String> {
    private String currentUsername;
    public void setCurrentUsername(String username) {
        this.currentUsername = username;
    }
    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            setStyle("-fx-padding: 0; -fx-background-color: transparent; -fx-border-width: 0; -fx-min-height: 0; -fx-pref-height: 0;");
            return;
        }
        String regex = "^\\[(.*?)\\] (.*?): (.*)$";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(item);
        String formattedTime = "";
        String sender;
        String message = item;
        if (matcher.matches()) {
            String dateHour = matcher.group(1);
            sender = matcher.group(2);
            message = matcher.group(3);
            try {
                java.time.LocalDateTime date = java.time.LocalDateTime.parse(dateHour);
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
                formattedTime = date.format(formatter);
            } catch (Exception ex) {
                formattedTime = dateHour;
            }
        } else {
            sender = "";
        }
        boolean isOwn = currentUsername != null && sender.equals(currentUsername);
        if (message.equals("[Mensaje de voz]")) {
            Label audioLabel = new Label("[Mensaje de voz]");
            audioLabel.setStyle("-fx-font-size: 15px; -fx-text-fill: " + (isOwn ? "white" : "#222") + ";");
            javafx.scene.control.Button playButton = new javafx.scene.control.Button("▶");
            playButton.setStyle("-fx-background-radius: 50%; -fx-background-color: #d2e7ff; -fx-font-size: 13px; -fx-padding: 2 8 2 8;");
            playButton.setFocusTraversable(false);
            playButton.setOnAction(e -> {
                try {
                    // Buscar el archivo usando el path exacto guardado en audioFilePath (del historial)
                    // Se asume que el objeto Message está accesible o que el path puede ser pasado/obtenido de alguna forma
                    // Aquí, se recomienda usar una función/callback para obtener el audioFilePath real del mensaje
                    String audioFilePath = null;
                    if (getItem() != null && getItem().contains("audioFilePath")) {
                        // Si el string del item contiene el path, extraerlo con regex json
                        java.util.regex.Pattern pathPattern = java.util.regex.Pattern.compile("\\\"audioFilePath\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
                        java.util.regex.Matcher pathMatcher = pathPattern.matcher(getItem());
                        if (pathMatcher.find()) {
                            audioFilePath = pathMatcher.group(1);
                        }
                    }
                    if (audioFilePath == null) {
                        // Fallback a la lógica anterior (por compatibilidad)
                        String audioRegex = "^\\[(.*?)\\] (.*?): \\[Mensaje de voz\\]$";
                        java.util.regex.Pattern p = java.util.regex.Pattern.compile(audioRegex);
                        java.util.regex.Matcher m = p.matcher(item);
                        if (m.matches()) {
                            String dateHour = m.group(1);
                            String senderName = m.group(2);
                            String safeDate = dateHour.replaceAll("[:.]", "-");
                            System.out.println("safeDate: " + safeDate);
                            audioFilePath = "history/audio/" + senderName + "_" + safeDate + ".wav";
                        }
                    }
                    if (audioFilePath != null) {
                        String wavPath = "userdata/" + (isOwn ? currentUsername : sender) + "/" + audioFilePath;
                        java.io.File wavFile = new java.io.File(wavPath);
                        if (!wavFile.exists()) {
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                            alert.setTitle("Audio no encontrado");
                            alert.setHeaderText(null);
                            alert.setContentText("No se encontró el archivo de audio: " + wavFile.getAbsolutePath());
                            alert.showAndWait();
                            return;
                        }
                        javax.sound.sampled.AudioInputStream audioStream = javax.sound.sampled.AudioSystem.getAudioInputStream(wavFile);
                        javax.sound.sampled.Clip clip = javax.sound.sampled.AudioSystem.getClip();
                        clip.open(audioStream);
                        clip.start();
                    } else {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                        alert.setTitle("Error de reproducción");
                        alert.setHeaderText(null);
                        alert.setContentText("No se pudo determinar la ruta del archivo de audio.");
                        alert.showAndWait();
                    }
                } catch (Exception ex) {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Error de reproducción");
                    alert.setHeaderText(null);
                    alert.setContentText("No se pudo reproducir el audio: " + ex.getMessage());
                    alert.showAndWait();
                }
            });
            HBox msgBox = new HBox(8, audioLabel, playButton);
            msgBox.setAlignment(Pos.CENTER_LEFT);
            msgBox.setSpacing(4);
            double bubbleWidth = 200;
            Text textHour = new Text(formattedTime);
            textHour.setStyle("-fx-font-size: 10px; -fx-fill: " + (isOwn ? "#e0e0e0" : "#888") + ";");
            StackPane stack = new StackPane(msgBox);
            StackPane.setAlignment(msgBox, Pos.CENTER_LEFT);
            StackPane.setAlignment(textHour, Pos.BOTTOM_RIGHT);
            stack.getChildren().add(textHour);
            stack.setMinWidth(bubbleWidth);
            stack.setMaxWidth(bubbleWidth);
            stack.setMinHeight(32);
            stack.setStyle("-fx-padding: 6 12 6 12;");
            Region bubbleBg = new Region();
            bubbleBg.setMinWidth(bubbleWidth);
            bubbleBg.setMaxWidth(bubbleWidth);
            bubbleBg.setMinHeight(32);
            bubbleBg.setStyle("-fx-background-color: " + (isOwn ? "#4f8cff" : "#ededed") + "; -fx-background-radius: 14px; -fx-effect: dropshadow(gaussian, #e0e0e0, 2, 0, 0, 1);");
            StackPane bubble = new StackPane(bubbleBg, stack);
            bubble.setMinWidth(bubbleWidth);
            bubble.setMaxWidth(bubbleWidth);
            bubble.setStyle("-fx-padding: 0; -fx-font-size: 15px;");
            setGraphic(bubble);
            setText(null);
            setStyle("-fx-alignment: " + (isOwn ? "CENTER-RIGHT" : "CENTER-LEFT") + "; -fx-padding: 4 20 4 20; -fx-background-color: transparent; -fx-border-width: 0;");
            return;
        }
        // Mensaje de texto normal
        Text textMsg = new Text(message);
        textMsg.setStyle("-fx-fill: " + (isOwn ? "white" : "#222") + "; -fx-font-size: 15px;");
        Text textHour = new Text(formattedTime);
        textHour.setStyle("-fx-font-size: 10px; -fx-fill: " + (isOwn ? "#e0e0e0" : "#888") + ";");
        HBox msgBox = new HBox();
        msgBox.setAlignment(Pos.CENTER_LEFT);
        msgBox.setSpacing(4);
        double textWidth = textMsg.getLayoutBounds().getWidth();
        double minBubbleWidth = 60;
        double maxBubbleWidth = 340;
        double bubbleWidth = Math.max(minBubbleWidth, Math.min(textWidth + 60, maxBubbleWidth));
        TextFlow msgFlow = new TextFlow(textMsg);
        msgFlow.setMaxWidth(bubbleWidth - 40);
        msgFlow.setStyle("-fx-padding: 0; -fx-background-color: transparent;");
        StackPane stack = new StackPane(msgFlow);
        StackPane.setAlignment(msgFlow, Pos.CENTER_LEFT);
        Text textHourCopy = new Text(formattedTime);
        textHourCopy.setStyle(textHour.getStyle());
        StackPane.setAlignment(textHourCopy, Pos.BOTTOM_RIGHT);
        stack.getChildren().add(textHourCopy);
        stack.setMinWidth(bubbleWidth);
        stack.setMaxWidth(bubbleWidth);
        stack.setMinHeight(32);
        stack.setStyle("-fx-padding: 6 12 6 12;");
        Region bubbleBg = new Region();
        bubbleBg.setMinWidth(bubbleWidth);
        bubbleBg.setMaxWidth(bubbleWidth);
        bubbleBg.setMinHeight(32);
        bubbleBg.setStyle("-fx-background-color: " + (isOwn ? "#4f8cff" : "#ededed") + "; -fx-background-radius: 14px; -fx-effect: dropshadow(gaussian, #e0e0e0, 2, 0, 0, 1);");
        StackPane bubble = new StackPane(bubbleBg, stack);
        bubble.setMinWidth(bubbleWidth);
        bubble.setMaxWidth(bubbleWidth);
        bubble.setStyle("-fx-padding: 0; -fx-font-size: 15px;");
        setGraphic(bubble);
        setStyle("-fx-alignment: " + (isOwn ? "CENTER-RIGHT" : "CENTER-LEFT") + "; -fx-padding: 4 20 4 20; -fx-background-color: transparent; -fx-border-width: 0;");
        setText(null);
    }
}
