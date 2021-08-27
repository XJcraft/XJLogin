package org.xjcraft.login.manager.impl;

import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.xjcraft.login.bean.Account;
import org.xjcraft.login.manager.Manager;

import java.sql.Timestamp;

public class BungeeImpl extends Manager {
    private org.xjcraft.login.Bungee plugin;

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
            updateAccount(account);
            player.sendMessage(ChatColor.YELLOW + "注册成功，即将转移……");
            sendPlayer(player, "main");

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
        player.sendMessage(ChatColor.YELLOW + "登陆成功！正在转移……");
        return true;
    }
}
