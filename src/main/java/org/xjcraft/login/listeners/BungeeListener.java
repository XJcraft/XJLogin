package org.xjcraft.login.listeners;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.bungee.event.message.passive.MiraiFriendMessageEvent;
import me.dreamvoid.miraimc.bungee.event.message.passive.MiraiGroupMessageEvent;
import me.dreamvoid.miraimc.bungee.event.message.passive.MiraiGroupTempMessageEvent;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import org.xjcraft.login.Bungee;
import org.xjcraft.login.manager.Manager;

import java.util.List;

import static org.xjcraft.login.bean.Constant.CHANNEL;

public class BungeeListener implements Listener {
    private Bungee plugin;
    private Manager manager;

    public BungeeListener(Bungee plugin, Manager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler
    public void login(ServerConnectedEvent event) {
        if (event.getServer().getInfo().getName().equalsIgnoreCase("login")) {
            event.getPlayer().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("输入/l <password>来登陆").color(ChatColor.YELLOW).create());
            event.getPlayer().sendMessage(ChatMessageType.CHAT, new ComponentBuilder("输入/l <password>来登陆").color(ChatColor.YELLOW).create());
        }
    }

    @EventHandler
    public void chat(MiraiGroupTempMessageEvent event) {
        if (event.getGroupID() == 225962968L) {
            String msg = doCommand(event.getMessage(), event.getMember().getId());
            event.getMember().sendMessage(msg);

        }
    }

    private String doCommand(String message, Long id) {
        System.out.println(message);
        try {
            long l = Long.parseLong(message);
            return manager.bindQQ(id, message);
        } catch (NumberFormatException e) {
//            event.getMember().sendMessage("格式错误！");
            return "格式错误！";
        }
    }

    @EventHandler
    public void chat(MiraiFriendMessageEvent event) {
        String msg = doCommand(event.getMessage(), event.getSenderID());
        event.getFriend().sendMessage(msg);
    }

    @EventHandler
    public void chat(MiraiGroupMessageEvent event) {
        if (event.getGroupID() == 225962968L) {
            System.out.println(event.getMessage());
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                if (player.getServer().getInfo().getName().equals("main")) {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
//                    out.writeUTF(SUB_CHANNEL_INCOME);
                    out.writeUTF("<" + event.getSenderNameCard() + "> " + event.getMessage());
                    player.getServer().getInfo().sendData(CHANNEL, out.toByteArray());
                }
            }


        }
    }

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (CHANNEL.equals(event.getTag())) {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            String name = in.readUTF();
            String text = in.readUTF();
            List<Long> onlineBots = MiraiBot.getOnlineBots();
            if (onlineBots.size() > 0) {
                MiraiBot bot = MiraiBot.getBot(onlineBots.get(0));
                MiraiGroup group = bot.getGroup(225962968L);
                group.sendMessage("<" + name + "> " + text);
            }

        }
    }
}
