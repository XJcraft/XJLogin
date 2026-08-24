package org.xjcraft.login.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.xjcraft.login.Spigot;
import org.xjcraft.login.bean.Account;
import org.xjcraft.login.manager.Manager;

import java.util.*;

import static org.xjcraft.login.bean.Constant.CHANNEL;

@Slf4j
public class SpigotListener implements Listener, PluginMessageListener {
    private Spigot plugin;
    private final Manager manager;
    private final Map<String, List<String>> duplicates = new HashMap<>();

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
        List<String> old = duplicates.computeIfAbsent(name, s -> new ArrayList<>());
        boolean duplicate = old.stream().anyMatch(s -> Objects.equals(s, message));

        if (duplicate) {
            event.getPlayer().sendMessage("请勿发送重复的消息！");
            log.info("player {} fail to send message due to duplicate", event.getPlayer().getName());
            return;
        } else {
            old.add(message);
            if (old.size() > 20) {
                old.remove(0);
            }
        }
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
