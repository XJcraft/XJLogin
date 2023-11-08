package org.xjcraft.login.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.xjcraft.login.Spigot;
import org.xjcraft.login.bean.Account;
import org.xjcraft.login.manager.Manager;

import static org.xjcraft.login.bean.Constant.CHANNEL;

public class SpigotListener implements Listener, PluginMessageListener {
    private Spigot plugin;
    private final Manager manager;

    public SpigotListener(Spigot plugin, Manager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (CHANNEL.equals(channel)) {
            ByteArrayDataInput in = ByteStreams.newDataInput(message);
            String text = in.readUTF();
//            Bukkit.broadcastMessage(text);
            Bukkit.getConsoleSender().sendMessage(text);
            for (Player p : Bukkit.getServer().getOnlinePlayers()) {
                Account account = manager.getCachedAccount(p.getName());
                if (account.getMute()) continue;
                p.sendMessage(text);
            }
        }
    }

    @EventHandler
    public void chat(AsyncPlayerChatEvent event) {
        String name = event.getPlayer().getName();
        String message = event.getMessage();
        Account account = manager.getCachedAccount(name);
        if (account.getHide()) return;
//        message = message.substring(1);
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(name);
        out.writeUTF(message);
        event.getPlayer().sendPluginMessage(plugin, CHANNEL, out.toByteArray());
    }

    public synchronized void addMessage(String message) {
        plugin.getServer().getOnlinePlayers().stream().findAny().ifPresent(player -> {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("XJCraft");
            out.writeUTF(message);
            player.sendPluginMessage(plugin, CHANNEL, out.toByteArray());
        });
    }
}
