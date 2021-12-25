package org.xjcraft.login.manager.impl;

import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.xjcraft.login.bean.Account;
import org.xjcraft.login.manager.Manager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SpigotImpl extends Manager implements CommandExecutor, TabCompleter {
    private Plugin plugin;
    List<String> names;
    private boolean isMainServer = true;

    public SpigotImpl(Plugin plugin, HikariDataSource source) {
        super(source);
        this.plugin = plugin;
        names = cachePlayerNames();
        plugin.getLogger().info(String.format("%s records cached", names.size()));
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String s, String[] args) {
        if (args.length == 0 || "help".equalsIgnoreCase(args[0])) {
            return help(sender);
        } else if (sender.isOp() && isMainServer) {
            switch (args[0]) {

                case "create":
                    if (args.length > 2)
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> createAccount(sender, args));
                    return true;
                case "edit":
                    if (args.length > 2)
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> editAccount(sender, args));
                    return true;
                case "status":
                    if (args.length > 1)
                        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> statusAccount(sender, args[1]));
                    return true;
            }
        }
        switch (args[0]) {
            case "chgpw":
            case "r":
            case "register":
                if (args.length > 1)
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> changePassword(sender, args));
                return true;
            case "invite":
                if (args.length > 1)
                    plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> invite(sender, args));
                return true;

        }
        return false;
    }


    private void editAccount(CommandSender sender, String[] args) {
        String name = args[1];
        String pwd = args[2];
        for (OfflinePlayer offlinePlayer : plugin.getServer().getOperators()) {
            if (offlinePlayer.getName().equals(name)) {
                sender.sendMessage("不允许修改管理员密码！");
                return;
            }
        }
        changePassword(name, pwd);
        sender.sendMessage("密码已修改！");
    }

    private void statusAccount(CommandSender sender, String arg) {
        Account account = getAccount(arg);
        if (account == null) {
            sender.sendMessage(ChatColor.YELLOW + "玩家不存在!");
            return;
        }
        sender.sendMessage(ChatColor.BLUE + "=====================================================");
        sender.sendMessage(ChatColor.YELLOW + "玩家：" + account.getName());
        sender.sendMessage(ChatColor.YELLOW + "上次登陆：" + account.getLastAction());
        sender.sendMessage(ChatColor.YELLOW + "登陆失败：" + account.getLoginFails());
        sender.sendMessage(ChatColor.YELLOW + "曾用ip：" + account.getIps());
        sender.sendMessage(ChatColor.YELLOW + "过期：" + account.getPasswordExpired());
        sender.sendMessage(ChatColor.BLUE + "=====================================================");
    }

    private void createAccount(CommandSender sender, String[] args) {
        String name = args[1];
        String pwd = args[2];
        Account account = getAccount(name);
        if (account != null) {
            sender.sendMessage(ChatColor.YELLOW + "玩家已存在!");
            return;
        }
        account = new Account(name, pwd);
        updateAccount(account);
        names.add(name);
        sender.sendMessage(ChatColor.YELLOW + "创建成功!");
    }

    private void changePassword(CommandSender sender, String[] strings) {
        if (sender instanceof Player) {
            String name = sender.getName();
            changePassword(name, strings[1]);
            sender.sendMessage("密码已修改！");
        }
    }

    private void invite(CommandSender sender, String[] args) {
        String name = args[1];
        Account inviter = getAccount(sender.getName());
        if (inviter.getPlayerType() != 1) {
            sender.sendMessage(ChatColor.YELLOW + "仅XJ建筑服可用");
            return;
        }

        Account account = getAccount(name);
        if (account != null) {
            sender.sendMessage(ChatColor.YELLOW + "玩家已存在!");
            return;
        }
        long l = new Random(System.currentTimeMillis()).nextLong();
        account = new Account(name, l + "");
        account.setPlayerType(1);
        account.setInviter(sender.getName());
        updateAccount(account);
        names.add(name);
        sender.sendMessage(ChatColor.YELLOW + "创建成功!密码为：" + l + "请提醒被邀请者及时更新密码！");
    }

    private void changePassword(String name, String passwrod) {
        Account account = getAccount(name);
        if (account == null) return;
        account.setPassword(passwrod);
        updateAccount(account);
    }

    private boolean help(CommandSender commandSender) {
        commandSender.sendMessage("/xl help 帮助");
        commandSender.sendMessage("/xl chgpw|register|r <password> 修改密码");
        commandSender.sendMessage("/xl invite <name> 邀请玩家（仅XJ建筑服可用！）");
        if (commandSender.isOp() && isMainServer) {
            commandSender.sendMessage("/xl create <player> <password> 创建用户");
            commandSender.sendMessage("/xl edit <player> <password> 修改用户密码");
            commandSender.sendMessage("/xl status <player> 查看用户");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings) {
        ArrayList<String> list = new ArrayList<>();
        switch (strings.length) {
            case 0:
            case 1:
                list.add("help");
                list.add("chgpw");
                list.add("r");
                list.add("register");
                list.add("invite");
                if (commandSender.isOp() && isMainServer) {
                    list.add("create");
                    list.add("status");
                    list.add("edit");

                }
                if (strings.length > 0) {
                    Iterator<String> iterator = list.iterator();
                    while (iterator.hasNext()) {
                        String next = iterator.next();
                        if (!next.startsWith(strings[0])) {
                            iterator.remove();
                        }
                    }
                }
                return list;
            case 2:
                switch (strings[0]) {
                    case "create":
                    case "status":
                    case "edit":
                        ArrayList<String> pattenList = new ArrayList<>();
                        if (strings.length > 1) {
                            for (String name : names) {
                                if (name.startsWith(strings[1])) {
                                    pattenList.add(name);
                                }
                            }
                            return pattenList;
                        }
                        return names;
                    case "chgpw":
                    case "r":
                    case "register":
                        return new ArrayList<String>() {{
                            add("password");
                        }};
                    case "invite":
                        return new ArrayList<String>() {{
                            add("playerName");
                        }};
                }
            case 3:
                return new ArrayList<String>() {{
                    add("password");
                }};
        }
        return list;
    }


}
