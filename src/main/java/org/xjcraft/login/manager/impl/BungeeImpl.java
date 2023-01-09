package org.xjcraft.login.manager.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.xjcraft.login.bean.Account;
import org.xjcraft.login.manager.Manager;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class BungeeImpl extends Manager {
    private org.xjcraft.login.Bungee plugin;
    private Cache<String, Account> cache = CacheBuilder.newBuilder()
            //限制缓存大小，防止OOM
            .maximumSize(100)
            //提供过期策略
            .expireAfterAccess(3, TimeUnit.MINUTES)
            //缓存不存在的时候，自动加载
            .build();

    public BungeeImpl(org.xjcraft.login.Bungee plugin, HikariDataSource source) {
        super(source);
        this.plugin = plugin;
    }

    public void login(ProxiedPlayer player, String[] args) {
        if (args.length == 0) {
            player.chat("输入/login <passcode> 来登陆");
        }

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            Account account = getAccount(player.getName());

            if (validAccount(player, args[0], account)) {
                String ip = player.getAddress().getHostString();
                Integer playerType = account.getPlayerType();
                switch (playerType) {
                    case 1:
                        sendPlayer(player, "official");
                        break;
                    default:
                        sendPlayer(player, "main");
                        break;
                }

                account.setLastAction(new Timestamp(System.currentTimeMillis()));
                account.setLoginFails(0);
                if (!account.getIps().contains(ip)) {
                    account.setIps(account.getIps() + (account.getIps().length() > 0 ? "," : "") + ip);
                }
            } else {
                if (account == null) return;
                account.setLoginFails(account.getLoginFails() + 1);
            }
            updateAccount(account);


        });

    }

    private void sendPlayer(ProxiedPlayer player, String server) {
        ServerInfo target = ProxyServer.getInstance().getServerInfo(server);
        player.connect(target);
    }

    public void register(ProxiedPlayer player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.YELLOW + "输入/register <password> 来注册");
        }

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            Account account = getAccount(player.getName());

            if (account != null) {
                player.sendMessage(ChatColor.YELLOW + "已有账号，请用/login <password>登陆");
                return;
            }
            account = new Account(player.getName(), args[0]);
//            updateAccount(account);
//            player.sendMessage(ChatColor.YELLOW + "注册成功，即将转移……");
//            sendPlayer(player, "main");
            createValidCode(player, account);

        });

    }


    private boolean validAccount(ProxiedPlayer player, String arg, Account account) {
        if (account == null) {
            player.sendMessage(ChatColor.YELLOW + "账号不存在！请使用/register来注册");
            return false;
        }
        if (account.getPasswordExpired()) {
            player.sendMessage(ChatColor.YELLOW + "账号过期！");
            return false;
        }
        if (!account.getPassword().equals(arg)) {
            player.sendMessage(ChatColor.YELLOW + "密码错误！");
            return false;
        }
        if (account.getQq() == null || account.getQq() == 0) {
            createValidCode(player, account);
            return false;
        }
        player.sendMessage(ChatColor.YELLOW + "登陆成功！正在转移……");
        return true;
    }

    private void createValidCode(ProxiedPlayer player, Account account) {
        int i = (int) (new Random().nextFloat() * 1000000);
        String code = String.format("%06d", i);
        cache.put(code, account);
        player.sendMessage(ChatColor.YELLOW + String.format("尚未绑定qq！请在3分钟内群内向认证机器人(2289537061)发送验证码<%s>！", code));
    }

    @Override
    public String bindQQ(long id, String message) {

        Account account = cache.getIfPresent(message);
        if (account != null) {
            account.setQq(id);
            String hasDuplicate = null;
            try {
                hasDuplicate = findQq(id);
            } catch (SQLException e) {
                e.printStackTrace();
                return "数据异常！请联系OP！";
            }
            if (hasDuplicate != null) {
                return "绑定失败！你的qq已经绑定了其他账号！";
            }
            updateAccount(account);
            cache.invalidate(message);
            return "绑定成功！";
        }
        return "绑定失败！请重试！";
    }


}
