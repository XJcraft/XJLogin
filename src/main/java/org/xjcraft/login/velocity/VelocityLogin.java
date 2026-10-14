package org.xjcraft.login.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.xjcraft.login.velocity.command.VelocityCommandLogin;
import org.xjcraft.login.velocity.command.VelocityCommandRegister;
import org.xjcraft.login.velocity.listener.VelocityLoginListener;
import org.xjcraft.login.velocity.manager.VelocityManagerImpl;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.xjcraft.login.bean.Constant.CHANNEL;

@Plugin(
        id = "xjlogin",
        name = "XJLogin",
        version = "1.0.2",
        description = "XJ服务器登录插件",
        authors = {"Ree"}
)
public class VelocityLogin {
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private HikariDataSource hikariDataSource;
    private VelocityLoginListener listener;

    @Inject
    public VelocityLogin(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 初始化配置文件
        File configFile = dataDirectory.resolve("config.yml").toFile();
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("无法创建数据目录", e);
                return;
            }
        }

        if (!configFile.exists()) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (IOException e) {
                logger.error("无法复制默认配置文件", e);
                return;
            }
        }

        try {
            // 加载配置
            Yaml yaml = new Yaml();
            Map<String, Object> config;
            try (InputStream in = Files.newInputStream(configFile.toPath())) {
                config = yaml.load(in);
            }

            hikariDataSource = new HikariDataSource(loadConfig(config));
            VelocityManagerImpl manager = new VelocityManagerImpl(this, hikariDataSource);
            listener = new VelocityLoginListener(this, manager);

            // 注册监听器
            proxy.getEventManager().register(this, listener);

            // 注册命令
            proxy.getCommandManager().register(
                    proxy.getCommandManager().metaBuilder("login")
                            .aliases("l")
                            .plugin(this)
                            .build(),
                    new VelocityCommandLogin(manager)
            );

            proxy.getCommandManager().register(
                    proxy.getCommandManager().metaBuilder("register")
                            .aliases("r")
                            .plugin(this)
                            .build(),
                    new VelocityCommandRegister(manager)
            );

            // 注册插件消息通道
            MinecraftChannelIdentifier channelIdentifier = MinecraftChannelIdentifier.from(CHANNEL);
            proxy.getChannelRegistrar().register(channelIdentifier);

            logger.info("XJLogin 已加载");

        } catch (Exception e) {
            logger.error("加载配置失败", e);
        }
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (listener != null) {
            listener.cancelTask();
        }
        if (hikariDataSource != null) {
            hikariDataSource.close();
        }
    }

    private static HikariConfig loadConfig(Map<String, Object> config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName("org.mariadb.jdbc.Driver");

        @SuppressWarnings("unchecked")
        Map<String, Object> dataSource = (Map<String, Object>) config.get("dataSource");

        hikariConfig.setJdbcUrl((String) dataSource.get("url"));
        hikariConfig.setUsername((String) dataSource.get("userName"));
        hikariConfig.setPassword((String) dataSource.get("password"));
        hikariConfig.setConnectionTimeout(3000);
        hikariConfig.setIdleTimeout(60000);
        hikariConfig.setMaxLifetime(1800000);
        hikariConfig.setMinimumIdle(1);
        hikariConfig.setMaximumPoolSize(10);
        return hikariConfig;
    }

    public ProxyServer getProxy() {
        return proxy;
    }

    public Logger getLogger() {
        return logger;
    }
}
