package org.xjcraft.login.manager;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.zaxxer.hikari.HikariDataSource;
import org.xjcraft.login.bean.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class Manager {

    private HikariDataSource source;
    LoadingCache<String, Account> cache;
    public Manager(HikariDataSource source) {
        this.source = source;
        try {
            initTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        CacheLoader<String, Account> loader = new CacheLoader<String, Account>() {
            @Override
            public Account load(String key) {
                return getAccount(key);
            }
        };

        cache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(loader);
    }

    private void initTable() throws SQLException {
        try (Connection connection = source.getConnection()) {
            PreparedStatement statement = connection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS `CrazyLogin_accounts` (\n" +
                            "  `name` char(255) CHARACTER SET utf8mb3 COLLATE utf8mb3_bin NOT NULL,\n" +
                            "  `password` char(255) NOT NULL,\n" +
                            "  `ips` text NOT NULL,\n" +
                            "  `lastAction` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),\n" +
                            "  `loginFails` int(11) NOT NULL DEFAULT 0,\n" +
                            "  `passwordExpired` bit(1) NOT NULL DEFAULT b'0',\n" +
                            "  `playerType` int(11) unsigned DEFAULT 0,\n" +
                            "  `inviter` varchar(255) DEFAULT NULL,\n" +
                            "  `qq` int(15) DEFAULT NULL,\n" +
                            "  `mute` bit(1) NOT NULL DEFAULT b'0',\n" +
                            "  `hide` bit(1) NOT NULL DEFAULT b'0',\n" +
                            "  PRIMARY KEY (`name`) USING BTREE,\n" +
                            "  UNIQUE KEY `qq` (`qq`) USING BTREE,\n" +
                            "  KEY `name` (`name`) USING BTREE\n" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb3 COLLATE=utf8mb3_general_ci ROW_FORMAT=DYNAMIC;");
            statement.execute();
        }
    }

    public String findQq(long id) throws SQLException {
        try (Connection connection = source.getConnection()) {

            PreparedStatement statement = connection.prepareStatement("select `name` from `CrazyLogin_accounts` " +
                    "where `qq`=?;");
            statement.setLong(1, id);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }

    protected void updateAccount(Account account) {
        if (account == null) return;
        try (Connection connection = source.getConnection()) {

            PreparedStatement statement = connection.prepareStatement("INSERT INTO `CrazyLogin_accounts` " +
                    "( `name`, `password`, `ips`, `lastAction`, `loginFails`, `passwordExpired`, `playerType`, `inviter`,`qq` ,`mute`,`hide`) VALUES " +
                    "( ?, ?, ?, ?, ?, ? , ?, ? ,? , ? ,? )  ON DUPLICATE KEY UPDATE " +
                    "`password` = ?," +
                    " `ips` =?," +
                    " `lastAction` = ?," +
                    " `loginFails` = ?, " +
                    "`passwordExpired` = ?," +
                    " `playerType` = ?, " +
                    " `inviter` = ?, " +
                    "`qq` = ?," +
                    "`mute` = ?," +
                    "`hide` = ?" +
                    ";");
            statement.setString(1, account.getName());
            statement.setString(2, account.getPassword());
            statement.setString(3, account.getIps());
            statement.setTimestamp(4, account.getLastAction());
            statement.setInt(5, account.getLoginFails());
            statement.setBoolean(6, account.getPasswordExpired());
            statement.setInt(7, account.getPlayerType());
            statement.setString(8, account.getInviter());
            if (account.getQq() != null) {
                statement.setLong(9, account.getQq());
            } else {
                statement.setNull(9, java.sql.Types.NULL);

            }
            statement.setBoolean(10, account.getMute());
            statement.setBoolean(11, account.getHide());
            statement.setString(12, account.getPassword());
            statement.setString(13, account.getIps());
            statement.setTimestamp(14, account.getLastAction());
            statement.setInt(15, account.getLoginFails());
            statement.setBoolean(16, account.getPasswordExpired());
            statement.setInt(17, account.getPlayerType());
            statement.setString(18, account.getInviter());
            if (account.getQq() != null) {
                statement.setLong(19, account.getQq());
            } else {
                statement.setNull(19, java.sql.Types.NULL);
            }
            statement.setBoolean(20, account.getMute());
            statement.setBoolean(21, account.getHide());
            statement.execute();
            cache.invalidate(account.getName());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Account getCachedAccount(String name) {
        try {
            return cache.get(name);
        } catch (ExecutionException e) {
            return null;
        }
    }
    protected Account getAccount(String name) {
        try (Connection connection = source.getConnection()) {

            PreparedStatement statement = connection.prepareStatement("select * from CrazyLogin_accounts where name=?");
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return new Account(
                        resultSet.getString("name"),
                        resultSet.getString("password"),
                        resultSet.getString("ips"),
                        resultSet.getTimestamp("lastAction"),
                        resultSet.getInt("loginFails"),
                        resultSet.getBoolean("passwordExpired"),
                        resultSet.getInt("playerType"),
                        resultSet.getString("inviter"),
                        resultSet.getLong("qq"),
                        resultSet.getBoolean("mute"),
                        resultSet.getBoolean("hide")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
        return null;
    }

    protected List<String> cachePlayerNames() {
        ArrayList<String> list = new ArrayList<>();
        try (Connection connection = source.getConnection()) {

            PreparedStatement statement = connection.prepareStatement("select `name` from CrazyLogin_accounts");
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                list.add(resultSet.getString(1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }


    public String bindQQ(long id, String message) {
        return "绑定失败！";
    }
}
