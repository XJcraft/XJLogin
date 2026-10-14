package org.xjcraft.login.velocity.manager;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.zaxxer.hikari.HikariDataSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.xjcraft.login.bean.Account;
import org.xjcraft.login.manager.Manager;
import org.xjcraft.login.velocity.VelocityLogin;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class VelocityManagerImpl extends Manager {
    private final VelocityLogin plugin;
    private final Cache<String, Account> cache = CacheBuilder.newBuilder()
            .maximumSize(100)
            .expireAfterAccess(3, TimeUnit.MINUTES)
            .build();

    public VelocityManagerImpl(VelocityLogin plugin, HikariDataSource source) {
        super(source);
        this.plugin = plugin;
    }

    public void login(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("输入/login <passcode> 来登陆", NamedTextColor.YELLOW));
            return;
        }

        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            Account account = getAccount(player.getUsername());

            if (validAccount(player, args[0], account)) {
                String ip = player.getRemoteAddress().getHostString();
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
        }).schedule();
    }

    private void sendPlayer(Player player, String serverName) {
        Optional<RegisteredServer> target = plugin.getProxy().getServer(serverName);
        target.ifPresent(server -> player.createConnectionRequest(server).connect());
    }

    public void register(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(Component.text("输入/register <password> 来注册", NamedTextColor.YELLOW));
            return;
        }

        plugin.getProxy().getScheduler().buildTask(plugin, () -> {
            Account account = getAccount(player.getUsername());

            if (account != null) {
                player.sendMessage(Component.text("已有账号，请用/login <password>登陆", NamedTextColor.YELLOW));
                return;
            }
            account = new Account(player.getUsername(), args[0]);
            createValidCode(player, account);
        }).schedule();
    }

    private boolean validAccount(Player player, String arg, Account account) {
        if (account == null) {
            player.sendMessage(Component.text("账号不存在！请使用/register来注册", NamedTextColor.YELLOW));
            return false;
        }
        if (account.getPasswordExpired()) {
            player.sendMessage(Component.text("账号过期！", NamedTextColor.YELLOW));
            return false;
        }
        if (!account.getPassword().equals(arg)) {
            player.sendMessage(Component.text("密码错误！", NamedTextColor.YELLOW));
            return false;
        }
        if (account.getQq() == null || account.getQq() == 0) {
            createValidCode(player, account);
            return false;
        }
        player.sendMessage(Component.text("登陆成功！正在转移……", NamedTextColor.YELLOW));
        return true;
    }

    private void createValidCode(Player player, Account account) {
        int i = (int) (new Random().nextFloat() * 1000000);
        String code = String.format("%06d", i);
        cache.put(code, account);
        player.sendMessage(Component.text(
                String.format("尚未绑定qq！请在3分钟内群内向认证机器人(2289537061)发送验证码<%s>！", code),
                NamedTextColor.YELLOW
        ));
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
