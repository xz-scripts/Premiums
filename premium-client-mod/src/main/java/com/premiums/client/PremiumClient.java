package com.premiums.client;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class PremiumClient implements ClientModInitializer {
    private static final String PREFIX = "*premium";

    @Override
    public void onInitializeClient() {
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!message.equals(PREFIX) && !message.startsWith(PREFIX + " ")) {
                return true;
            }

            handleCommand(message);
            return false;
        });

        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof ChatScreen)) {
                return;
            }

            ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, key) -> {
                if (key.key() != GLFW.GLFW_KEY_TAB) {
                    return true;
                }

                Optional<EditBox> input = screen.children().stream()
                        .filter(EditBox.class::isInstance)
                        .map(EditBox.class::cast)
                        .findFirst();
                if (input.isEmpty()) {
                    return true;
                }

                String completion = complete(input.get().getValue(), client);
                if (completion == null) {
                    return true;
                }

                input.get().setValue(completion);
                input.get().moveCursorToEnd(false);
                return false;
            });
        });
    }

    private static void handleCommand(String message) {
        String[] arguments = message.trim().split("\\s+");
        if (arguments.length == 1) {
            showHelp();
            return;
        }

        if (arguments.length == 2 && arguments[1].equals("get")) {
            showPremiumPlayers();
            return;
        }

        if (arguments.length == 3 && arguments[1].equals("check")) {
            checkPlayer(arguments[2]);
            return;
        }

        showHelp();
    }

    private static String complete(String input, Minecraft client) {
        if (PREFIX.startsWith(input) && input.length() < PREFIX.length()) {
            return PREFIX + " ";
        }
        if (input.equals(PREFIX)) {
            return PREFIX + " ";
        }
        if (!input.startsWith(PREFIX + " ")) {
            return null;
        }

        String arguments = input.substring(PREFIX.length() + 1);
        if (!arguments.contains(" ")) {
            return List.of("check", "get").stream()
                    .filter(command -> command.startsWith(arguments.toLowerCase(Locale.ROOT)))
                    .findFirst()
                    .map(command -> PREFIX + " " + command + " ")
                    .orElse(null);
        }

        if (!arguments.startsWith("check ") || client.getConnection() == null) {
            return null;
        }

        String namePrefix = arguments.substring("check ".length()).toLowerCase(Locale.ROOT);
        return client.getConnection().getOnlinePlayers().stream()
                .map(PlayerInfo::getProfile)
                .map(profile -> profile.name())
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(namePrefix))
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .findFirst()
                .map(name -> PREFIX + " check " + name + " ")
                .orElse(null);
    }

    private static void showHelp() {
        sendLocal("Comandos de Premium:");
        sendLocal("*premium              Muestra esta ayuda");
        sendLocal("*premium get          Lista candidatos premium conectados");
        sendLocal("*premium check <usuario> Comprueba un jugador conectado (TAB autocompleta)");
    }

    private static void showPremiumPlayers() {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            sendLocal("Conectate a un servidor para consultar los jugadores conectados.");
            return;
        }

        List<String> players = client.getConnection().getOnlinePlayers().stream()
                .filter(player -> player.getProfile().id().version() == 4)
                .map(PlayerInfo::getProfile)
                .map(profile -> profile.name())
                .sorted(Comparator.comparing(String::toLowerCase))
                .toList();

        if (players.isEmpty()) {
            sendLocal("No se encontraron candidatos premium conectados.");
        } else {
            sendLocal("Candidatos premium conectados (" + players.size() + "): " + String.join(", ", players));
        }

        sendLocal("Aviso: se estima usando UUID v4; algunos servidores o proxies pueden alterar el resultado.");
    }

    private static void checkPlayer(String username) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            sendLocal("Conectate a un servidor para consultar jugadores conectados.");
            return;
        }

        PlayerInfo player = client.getConnection().getOnlinePlayers().stream()
                .filter(entry -> entry.getProfile().name().equalsIgnoreCase(username))
                .findFirst()
                .orElse(null);

        if (player == null) {
            sendLocal(username + " no aparece en la lista de jugadores conectados.");
        } else if (player.getProfile().id().version() == 4) {
            sendLocal(username + " parece tener una cuenta premium (UUID v4).");
        } else {
            sendLocal(username + " no parece tener una cuenta premium (UUID no v4).");
        }
        sendLocal("La comprobacion es orientativa; servidores o proxies pueden alterar el resultado.");
    }

    private static void sendLocal(String message) {
        Minecraft client = Minecraft.getInstance();
        if (client.gui != null) {
            client.gui.hud.getChat().addClientSystemMessage(Component.literal(message));
        }
    }
}