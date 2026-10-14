package org.xjcraft.login.velocity.listener;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.scheduler.ScheduledTask;
import me.dreamvoid.miraimc.api.MiraiBot;
import me.dreamvoid.miraimc.api.bot.MiraiGroup;
import me.dreamvoid.miraimc.velocity.event.message.passive.MiraiFriendMessageEvent;
import me.dreamvoid.miraimc.velocity.event.message.passive.MiraiGroupMessageEvent;
import me.dreamvoid.miraimc.velocity.event.message.passive.MiraiGroupTempMessageEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.xjcraft.login.manager.Manager;
import org.xjcraft.login.util.StringUtil;
import org.xjcraft.login.velocity.VelocityLogin;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.xjcraft.login.bean.Constant.CHANNEL;

public class VelocityLoginListener {
    private final VelocityLogin plugin;
    private final Manager manager;
    private final List<String> chats = new ArrayList<>();
    private long timestamp = 0;
    private long cmdOnlineTimestamp = 0;
    private ScheduledTask task;
    private final AtomicInteger lock = new AtomicInteger();
    private final Cache<String, Boolean> receiveCache;
    private final Cache<String, Set<Long>> sendCache;
    private final MinecraftChannelIdentifier channelIdentifier;

    public VelocityLoginListener(VelocityLogin plugin, Manager manager) {
        this.plugin = plugin;
        this.manager = manager;
        this.channelIdentifier = MinecraftChannelIdentifier.from(CHANNEL);

        receiveCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.SECONDS).build();
        sendCache = CacheBuilder.newBuilder()
                .expireAfterAccess(10, TimeUnit.SECONDS)
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .removalListener((RemovalListener<String, Set<Long>>) notification -> {
                    if (notification.getCause() == RemovalCause.EXPIRED) {
                        try {
                            plugin.getLogger().info("resend messages to qq group:" + StringUtil.join(chats.toArray(), ";"));
                            MiraiBot.getOnlineBots().stream()
                                    .filter(id -> !notification.getValue().contains(id))
                                    .findAny()
                                    .ifPresent(aLong -> {
                                        MiraiBot bot = MiraiBot.getBot(aLong);
                                        bot.getGroup(225962968L).sendMessage(notification.getKey());
                                        plugin.getLogger().info("via bot:" + bot.getID());
                                    });
                        } catch (NoSuchElementException e) {
                            plugin.getLogger().warn("resend messages 失败，机器人已离线", e);
                        }
                    }
                })
                .build();
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (event.getServer().getServerInfo().getName().equalsIgnoreCase("login")) {
            event.getPlayer().sendActionBar(Component.text("输入/l <password>来登陆", NamedTextColor.YELLOW));
            event.getPlayer().sendMessage(Component.text("输入/l <password>来登陆", NamedTextColor.YELLOW));
        }
    }

    @Subscribe
    public void onGroupTempMessage(MiraiGroupTempMessageEvent event) {
        if (event.getGroupID() == 225962968L) {
            plugin.getLogger().debug("MiraiGroupTempMessageEvent: {}", Thread.currentThread().getName());
            String msg = doBind(event.getMessage(), event.getSenderID());
            event.getMember().sendMessage(msg);
        }
    }

    private String doBind(String message, Long id) {
        try {
            long l = Long.parseLong(message);
            return manager.bindQQ(id, message);
        } catch (NumberFormatException e) {
            return "格式错误！";
        }
    }

    @Subscribe
    public void onFriendMessage(MiraiFriendMessageEvent event) {
        String msg = doBind(event.getMessage(), event.getSenderID());
        event.getFriend().sendMessage(msg);
    }

    @Subscribe
    public void onGroupMessage(MiraiGroupMessageEvent event) throws ExecutionException {
        if (event.getGroupID() == 225962968L) {
            sendCache.invalidate(event.getMessage());
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

            synchronized (receiveCache) {
                String s = event.getSenderName() + event.getMessage();
                if (receiveCache.get(s, () -> false)) {
                    return;
                }
                receiveCache.put(s, true);
            }

            String name = null;
            try {
                String serverId = manager.findQq(event.getSenderID());
                if (serverId != null) name = serverId;
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (name == null) name = String.format("未绑定用户%s", event.getSenderID());

            for (Player player : plugin.getProxy().getAllPlayers()) {
                if (player.getCurrentServer().isPresent()) {
                    ServerConnection server = player.getCurrentServer().get();
                    if (server.getServerInfo().getName().equals("main")) {
                        ByteArrayDataOutput out = ByteStreams.newDataOutput();
                        String message = event.getMessage();
                        out.writeUTF("<" + name + "> " + message);
                        server.sendPluginMessage(channelIdentifier, out.toByteArray());
                        return;
                    }
                }
            }
        }
    }

    private void doCommand(MiraiGroupMessageEvent event) {
        if ("/online".equals(event.getMessage())) {
            if (System.currentTimeMillis() < cmdOnlineTimestamp) return;
            cmdOnlineTimestamp = System.currentTimeMillis() + 1000;
            StringBuilder result = new StringBuilder();

            for (RegisteredServer server : plugin.getProxy().getAllServers()) {
                Collection<Player> players = server.getPlayersConnected();
                if (players.size() > 0) {
                    result.append(String.format("%s(%d):%s\n",
                            server.getServerInfo().getName(),
                            players.size(),
                            StringUtil.join(players.stream().map(Player::getUsername).toArray(), ",")));
                }
            }

            if (result.length() > 0) {
                event.getGroup().sendMessage(result.toString());
            } else {
                event.getGroup().sendMessage("无人生还！");
            }
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        if (!channelIdentifier.equals(event.getIdentifier())) {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(event.getData());
        String name = in.readUTF();
        String text = in.readUTF();
        chats.add("<" + name + "> " + text);
        plugin.getLogger().debug("onPluginMessageReceived: {}", Thread.currentThread().getName());
        triggerMessageTimer();
    }

    private void triggerMessageTimer() {
        synchronized (lock) {
            if (System.currentTimeMillis() - timestamp > 1000 * 10) {
                plugin.getProxy().getScheduler().buildTask(plugin, this::sendChatHistory).schedule();
            } else if (task == null) {
                task = plugin.getProxy().getScheduler().buildTask(plugin, this::sendChatHistory)
                        .delay(1000 * 10 - System.currentTimeMillis() + timestamp, TimeUnit.MILLISECONDS)
                        .schedule();
            }
        }
        timestamp = System.currentTimeMillis();
    }

    public void sendChatHistory() {
        List<Long> onlineBots = MiraiBot.getOnlineBots();
        if (onlineBots.size() <= 0) {
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

    /**
     * 取消尚未执行的延迟发送任务，供插件卸载时调用。
     */
    public void cancelTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}
