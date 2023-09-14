package org.xjcraft.login;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import org.xjcraft.login.api.MessageAPI;
import org.xjcraft.login.command.CommandLogin;
import org.xjcraft.login.command.CommandRegister;
import org.xjcraft.login.listeners.BungeeListener;
import org.xjcraft.login.manager.impl.BungeeImpl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import static org.xjcraft.login.bean.Constant.CHANNEL;

public class Bungee extends Plugin {
    @Override
    public void onEnable() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            Configuration configuration = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
            HikariDataSource hikariDataSource = new HikariDataSource(loadConfig(configuration));
            BungeeImpl manager = new BungeeImpl(this, hikariDataSource);
            BungeeListener listener = new BungeeListener(this, manager);
            MessageAPI.setMessageManager(listener);
            getProxy().getPluginManager().registerListener(this, listener);
            getProxy().getPluginManager().registerCommand(this, new CommandLogin("login", manager));
            getProxy().getPluginManager().registerCommand(this, new CommandLogin("l", manager));
            getProxy().getPluginManager().registerCommand(this, new CommandRegister("register", manager));
            getProxy().getPluginManager().registerCommand(this, new CommandRegister("r", manager));
            getProxy().registerChannel(CHANNEL);

        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private static HikariConfig loadConfig(Configuration config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setJdbcUrl(config.getString("dataSource.url"));
        hikariConfig.setUsername(config.getString("dataSource.userName"));
        hikariConfig.setPassword(config.getString("dataSource.password"));
        hikariConfig.setConnectionTimeout(3000);
        hikariConfig.setIdleTimeout(60000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(10);
        return hikariConfig;
    }


}
