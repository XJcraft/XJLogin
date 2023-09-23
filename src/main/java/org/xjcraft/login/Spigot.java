package org.xjcraft.login;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.xjcraft.login.api.MessageAPI;
import org.xjcraft.login.listeners.SpigotListener;
import org.xjcraft.login.manager.impl.SpigotImpl;

import java.io.File;

import static org.xjcraft.login.bean.Constant.CHANNEL;

public class Spigot extends JavaPlugin {

    @Override
    public void onEnable() {
        if (!getDataFolder().exists())
            getDataFolder().mkdir();
        File file = new File(getDataFolder(), "config.yml");
        if (!file.exists()) {
            saveDefaultConfig();
        }
        FileConfiguration config = getConfig();
        saveConfig();
//        System.out.println(HikariConfig.class.getClassLoader());
//        System.out.println(HikariDataSource.class.getClassLoader());
//        System.out.println(this.getClassLoader());
        HikariDataSource hikariDataSource = new HikariDataSource(loadConfig(config));
        SpigotImpl manager = new SpigotImpl(this, hikariDataSource);
        getCommand("xl").setExecutor(manager);
        getCommand("xl").setTabCompleter(manager);
        SpigotListener listener = new SpigotListener(this);
        MessageAPI.setMessageManager(listener);
        this.getServer().getPluginManager().registerEvents(listener, this);
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, CHANNEL);
        this.getServer().getMessenger().registerIncomingPluginChannel(this, CHANNEL, listener);

    }

    @Override
    public void onDisable() {
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    private static HikariConfig loadConfig(FileConfiguration config) {
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
