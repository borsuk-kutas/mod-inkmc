package com.example.examplemod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

public class TemplateMod implements ModInitializer {

    // Konfiguracja nicków z Twojego serwera
    private static String nick1 = "Wixxaa";
    private static String nick2 = "Julcia69o";
    private static String nick3 = "Lolotr7";
    
    private static int menuKey = GLFW.GLFW_KEY_GRAVE_ACCENT; // Klawisz ~ (tylda) pod ESC
    private static int toggleKey = GLFW.GLFW_KEY_X;          // Klawisz włączania/wyłączania bota (X)

    private static boolean modEnabled = false;
    private static boolean hudEnabled = true;
    private static String hudPosition = "LEWA_GORA";

    private static final Map<String, String> statuses = new HashMap<>();
    private static String lastCheckedAdmin = "";
    
    private int tickCounter = 0;
    private int currentTargetIndex = 0;
    private boolean lastToggleState = false;

    @Override
    public void onInitialize() {
        statuses.put(nick1, "Brak danych");
        statuses.put(nick2, "Brak danych");
        statuses.put(nick3, "Brak danych");

        // Pętla bota wykonywana w grze
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            // Otwieranie menu pod ~
            if (GLFW.glfwGetKey(client.getWindow().getHandle(), menuKey) == GLFW.GLFW_PRESS && client.currentScreen == null) {
                client.setScreen(new ConfigScreen());
            }

            // Włączanie/Wyłączanie bota klawiszem X
            boolean isKeyPressed = GLFW.glfwGetKey(client.getWindow().getHandle(), toggleKey) == GLFW.GLFW_PRESS;
            if (isKeyPressed && !lastToggleState) {
                modEnabled = !modEnabled;
                client.player.sendMessage(Text.of("§7[Radar] Bot został: " + (modEnabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")), true);
            }
            lastToggleState = isKeyPressed;

            if (!modEnabled) return;

            tickCounter++;
            if (tickCounter >= 20) { // Co 1 sekunda
                tickCounter = 0;
                sendAdminCheckCommand(client);
            }

            handleDiggingLogic(client);
        });

        // Czytanie czatu (Bez kombinowania z mixinami)
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            handleChatMessage(message.getString());
        });

        // Wyświetlanie HUD na ekranie
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (!hudEnabled) return;
            renderHUD(drawContext);
        });
    }

    private void sendAdminCheckCommand(MinecraftClient client) {
        String[] admins = {nick1, nick2, nick3};
        String target = admins[currentTargetIndex];
        lastCheckedAdmin = target;
        
        client.player.networkHandler.sendCommand("is info " + target);
        currentTargetIndex = (currentTargetIndex + 1) % 3;
    }

    private void handleDiggingLogic(MinecraftClient client) {
        if (client.interactionManager == null) return;
        
        if (isAdminOnline()) {
            if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) client.crosshairTarget;
                client.interactionManager.updateBlockBreakingProgress(blockHit.getBlockPos(), blockHit.getSide());
                client.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    public static void handleChatMessage(String message) {
        String cleanMessage = message.replaceAll("§.", "");

        if (cleanMessage.contains("Ostatnio online:")) {
            String status = "Offline";
            if (cleanMessage.contains("Online")) {
                status = "Online";
            } else {
                int index = cleanMessage.indexOf("Ostatnio online:");
                status = cleanMessage.substring(index + 16).trim();
            }
            
            if (!lastCheckedAdmin.isEmpty()) {
                statuses.put(lastCheckedAdmin, status);
            }
        }
    }

    private static boolean isAdminOnline() {
        return "Online".equalsIgnoreCase(statuses.get(nick1)) ||
               "Online".equalsIgnoreCase(statuses.get(nick2)) ||
               "Online".equalsIgnoreCase(statuses.get(nick3));
    }

    private void renderHUD(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        
        int x = 10; int y = 10;
        if (hudPosition.equals("PRAWA_GORA")) { x = width - 150; y = 10; }
        else if (hudPosition.equals("LEWY_DOL")) { x = 10; y = height - 60; }
        else if (hudPosition.equals("PRAWY_DOL")) { x = width - 150; y = height - 60; }

        int colorOnline = 0x00FF00; int colorOffline = 0xFF0000;

        context.drawTextWithShadow(client.textRenderer, nick1 + ": " + statuses.get(nick1), x, y, "Online".equals(statuses.get(nick1)) ? colorOnline : colorOffline);
        context.drawTextWithShadow(client.textRenderer, nick2 + ": " + statuses.get(nick2), x, y + 10, "Online".equals(statuses.get(nick2)) ? colorOnline : colorOffline);
        context.drawTextWithShadow(client.textRenderer, nick3 + ": " + statuses.get(nick3), x, y + 20, "Online".equals(statuses.get(nick3)) ? colorOnline : colorOffline);

        String botStatusText = isAdminOnline() ? "§cadministracja czuwa (pewnie i tak ma cię w dupie wsm)" : "§aadministracja smacznie śpi";
        context.drawTextWithShadow(client.textRenderer, botStatusText, x, y + 35, 0xFFFFFF);
    }

    public static class ConfigScreen extends Screen {
        public ConfigScreen() { super(Text.of("Ustawienia Radaru")); }
        @Override
        protected void init() {
            this.addDrawableChild(ButtonWidget.builder(Text.of("Pozycja HUD: " + hudPosition), (btn) -> {
                if (hudPosition.equals("LEWA_GORA")) hudPosition = "PRAWA_GORA";
                else if (hudPosition.equals("PRAWA_GORA")) hudPosition = "PRAWY_DOL";
                else if (hudPosition.equals("PRAWY_DOL")) hudPosition = "LEWY_DOL";
                else hudPosition = "LEWA_GORA";
                btn.setMessage(Text.of("Pozycja HUD: " + hudPosition));
            }).dimensions(this.width / 2 - 100, this.height / 2 - 10, 200, 20).build());
        }
    }
}