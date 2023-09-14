package org.xjcraft.login.api;

import org.xjcraft.login.listeners.BungeeListener;

public class MessageAPI {
    private static BungeeListener listener;

    public static void sendQQMessage(String message) {
        listener.addMessage(message);
    }

    public static void setMessageManager(BungeeListener listener) {

        MessageAPI.listener = listener;
    }
}
