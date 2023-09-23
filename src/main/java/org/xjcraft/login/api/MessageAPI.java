package org.xjcraft.login.api;

import org.xjcraft.login.listeners.SpigotListener;

public class MessageAPI {
    private static SpigotListener listener;

    public static void sendQQMessage(String message) {
        listener.addMessage(message);
    }

    public static void setMessageManager(SpigotListener listener) {
        MessageAPI.listener = listener;
    }
}
