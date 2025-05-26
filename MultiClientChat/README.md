# MultiClientChat

Integrantes:

- Maria Fanny Giraldo - A00399948
- Jhon Hurtua Montenegro - A00373357
- Joseph Daniel Cardenas - A00401425
- Juan Camilo Rubio -  A00401838


**MultiClientChat** es una aplicación de chat multiusuario desarrollada en Java. Permite enviar mensajes de texto y de voz en tiempo real, tanto en conversaciones individuales como grupales. Incluye una interfaz gráfica basada en JavaFX y persistencia de historiales.

## Características principales

- **Mensajería en tiempo real**: Envía y recibe mensajes de texto y de voz.
- **Chats individuales y grupales**: Crea grupos, añade miembros y chatea con varios usuarios a la vez.
- **Historial persistente**: Guarda los mensajes y audios localmente en formato JSON y WAV.
- **Interfaz gráfica intuitiva**: Basada en JavaFX para una experiencia de usuario moderna.
- **Gestión de usuarios**: Autenticación básica y control de sesiones.
- **Soporte de mensajes de voz**: Graba, envía y reproduce mensajes de audio.

##  Estructura del proyecto

- `client`: Cliente JavaFX (UI y lógica del usuario).
- `server`: Servidor de chat multiusuario (conexiones, reenvío, almacenamiento).
- `common`: Código compartido (modelos, utilidades, serialización).
- `userdata`: Archivos generados (usuarios, historiales, audios).

##  Requisitos

- Java 11 o superior.
- Maven (para compilar y gestionar dependencias).
- JavaFX (puede requerir configuración adicional según el entorno).

## Instalación y ejecución

1. **Clona el repositorio**:

   ```bash
   git clone https://github.com/john-mh/MultiClientChat.git
   cd MultiClientChat
   ```

2. **Compila los módulos** (server, client y common):

   ```bash
   mvn clean install
   ```

3. **Ejecuta el servidor**:

   ```bash
   cd server
   mvn exec:java
   ```

4. **Ejecuta el cliente** (en otra terminal):

   ```bash
   cd client
   mvn clean javafx:run
   ```

5. **Inicia sesión** con un nombre de usuario ¡y comienza a chatear!

## Dependencias principales

- JavaFX
- Gson (serialización JSON)
- Maven


