package org.xjcraft.login.listeners;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
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
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import org.xjcraft.login.Bungee;
import org.xjcraft.login.manager.Manager;
import org.xjcraft.login.util.StringUtil;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.xjcraft.login.bean.Constant.CHANNEL;

public class BungeeListener implements Listener {
    private Bungee plugin;
    private Manager manager;
    private List<String> chats = new ArrayList<>();
    private long timestamp = 0;
    private long cmdOnlineTimestamp = 0;
    ScheduledTask task;
    private final AtomicInteger lock = new AtomicInteger();
    Cache<String, Boolean> receiveCache;
    Cache<String, Set<Long>> sendCache;

    public BungeeListener(Bungee plugin, Manager manager) {
        this.plugin = plugin;
        this.manager = manager;

        receiveCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.SECONDS).build();
        sendCache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .removalListener((RemovalListener<String, Set<Long>>) notification -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        MiraiBot.getOnlineBots().stream().filter(id -> !notification.getValue().contains(id)).findAny().ifPresent(aLong -> {
                            MiraiBot bot = MiraiBot.getBot(aLong);
                            bot.getGroup(225962968L).sendMessage(notification.getKey());
                            plugin.getLogger().info("resend  messages via " + bot.getID() + " to qq group:" + StringUtil.join(chats.toArray(), ";"));

                        });
                    }
                })
                .build();

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
            System.out.println("MiraiGroupTempMessageEvent：" + Thread.currentThread().getName());
            String msg = doBind(event.getMessage(), event.getMember().getId());
            event.getMember().sendMessage(msg);

        }
    }

    private String doBind(String message, Long id) {
//        System.out.println(message);
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
//        System.out.println("MiraiFriendMessageEvent：" + Thread.currentThread().getName());
        String msg = doBind(event.getMessage(), event.getSenderID());
        event.getFriend().sendMessage(msg);
    }

    @EventHandler
    public void chat(MiraiGroupMessageEvent event) throws ExecutionException {
        if (event.getGroupID() == 225962968L) {
//            System.out.println("MiraiGroupMessageEvent：" + Thread.currentThread().getName());
//            System.out.println(event.getMessage());
            try {
                if (MiraiBot.getBot(event.getSenderID()) != null) {
                    return;
                }
            } catch (NoSuchElementException ignored) {
            }
            if (event.getMessage().startsWith("/")) {
                doCommand(event);
                return;
            }
//            if (!event.getMessage().startsWith("#")) return;
//            List<Long> onlineBots = MiraiBot.getOnlineBots();
            String s = event.getSenderName() + event.getMessage();
            if (receiveCache.get(s, () -> false)) {
                return;
            }
            receiveCache.put(s, true);
//            if (event.getBotID() != onlineBots.get(0)) return;
            String name = null;
            try {
                String serverId = manager.findQq(event.getSenderID());
                if (serverId != null) name = serverId;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (name == null) name = String.format("未绑定用户%s", event.getSenderID());
            for (ProxiedPlayer player : ProxyServer.getInstance().getPlayers()) {
                if (player.getServer().getInfo().getName().equals("main")) {

                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
//                    out.writeUTF(SUB_CHANNEL_INCOME);
                    String message = event.getMessage();//.substring(1);
                    out.writeUTF("<" + name + "> " + message);
                    player.getServer().getInfo().sendData(CHANNEL, out.toByteArray());
                    return;
                }
            }


        }
    }

    private void doCommand(MiraiGroupMessageEvent event) {
        switch (event.getMessage()) {
            case "/online":
                if (System.currentTimeMillis() < cmdOnlineTimestamp) return;
                cmdOnlineTimestamp = System.currentTimeMillis() + 1000;
                StringBuilder result = new StringBuilder();
                Map<String, ServerInfo> servers = ProxyServer.getInstance().getServers();
                for (Map.Entry<String, ServerInfo> entry : servers.entrySet()) {
                    Collection<ProxiedPlayer> players = entry.getValue().getPlayers();
                    if (players.size() > 0) {
                        result.append(String.format("%s(%d):%s\n", entry.getValue().getName(), players.size(), StringUtil.join(players.stream().map(CommandSender::getName).toArray(), ",")));
                    }
                }
                if (result.length() > 0) {
                    event.getGroup().sendMessage(result.toString());
                } else {
                    event.getGroup().sendMessage("无人生还！");

                }
                break;
            default:
                break;

        }
    }

    @EventHandler
    public void onPluginMessageReceived(PluginMessageEvent event) {
        if (CHANNEL.equals(event.getTag())) {
            ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
            String name = in.readUTF();
            String text = in.readUTF();
            chats.add("<" + name + "> " + text);
            System.out.println("onPluginMessageReceived：" + Thread.currentThread().getName());
            triggerMessageTimer();


        }
    }

    private void triggerMessageTimer() {
        synchronized (lock) {
            if (System.currentTimeMillis() - timestamp > 1000 * 10) {
                plugin.getProxy().getScheduler().runAsync(plugin, this::sendChatHistory);
            } else if (task == null) {
                task = plugin.getProxy().getScheduler().schedule(plugin, () ->
                                plugin.getProxy().getScheduler().runAsync(plugin, this::sendChatHistory),
                        1000 * 10 - System.currentTimeMillis() + timestamp, TimeUnit.MILLISECONDS);

            }
        }
        timestamp = System.currentTimeMillis();
    }

    public void sendChatHistory() {
        List<Long> onlineBots = MiraiBot.getOnlineBots();
        if (onlineBots.size() <= 0) {
//            MiraiBot.getBot(2289537061L).doOnline();
            return;
        }

        synchronized (lock) {
            if (chats.size() <= 0) return;
            MiraiBot bot = MiraiBot.getBot(onlineBots.get(lock.addAndGet(1) % onlineBots.size()));
            MiraiGroup group = bot.getGroup(225962968L);

            String message = StringUtil.join(chats.toArray(), "\n");
            sendCache.put(message, Set.of(bot.getID()));
            group.sendMessage(message);
            plugin.getLogger().info("send " + chats.size() + " messages via " + bot.getID() + " to qq group:" + StringUtil.join(chats.toArray(), ";"));
            chats.clear();
            task = null;
        }
    }


}
