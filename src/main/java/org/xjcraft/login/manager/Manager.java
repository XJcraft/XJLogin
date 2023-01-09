package org.xjcraft.login.manager;

import com.zaxxer.hikari.HikariDataSource;
import org.xjcraft.login.bean.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Manager {

    private HikariDataSource source;

    public Manager(HikariDataSource source) {
        this.source = source;
        try {
            initTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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
            return null;
        }
    }

    protected void updateAccount(Account account) {
        if (account == null) return;
        try (Connection connection = source.getConnection()) {

            PreparedStatement statement = connection.prepareStatement("INSERT INTO `CrazyLogin_accounts` " +
                    "( `name`, `password`, `ips`, `lastAction`, `loginFails`, `passwordExpired`, `playerType`, `inviter`,`qq` ) VALUES " +
                    "( ?, ?, ?, ?, ?, ? , ?, ? ,? )  ON DUPLICATE KEY UPDATE " +
                    "`password` = ?," +
                    " `ips` =?," +
                    " `lastAction` = ?," +
                    " `loginFails` = ?, " +
                    "`passwordExpired` = ?," +
                    " `playerType` = ?, " +
                    " `inviter` = ?, " +
                    "`qq` = ?;");
            statement.setString(1, account.getName());
            statement.setString(2, account.getPassword());
            statement.setString(3, account.getIps());
            statement.setTimestamp(4, account.getLastAction());
            statement.setInt(5, account.getLoginFails());
            statement.setBoolean(6, account.getPasswordExpired());
            statement.setInt(7, account.getPlayerType());
            statement.setString(8, account.getInviter());
            statement.setLong(9, account.getQq());
            statement.setString(10, account.getPassword());
            statement.setString(11, account.getIps());
            statement.setTimestamp(12, account.getLastAction());
            statement.setInt(13, account.getLoginFails());
            statement.setBoolean(14, account.getPasswordExpired());
            statement.setInt(15, account.getPlayerType());
            statement.setString(16, account.getInviter());
            statement.setLong(17, account.getQq());
            statement.execute();

        } catch (SQLException e) {
            e.printStackTrace();
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
                        resultSet.getLong("qq")
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
