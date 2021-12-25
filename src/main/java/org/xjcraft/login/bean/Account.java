package org.xjcraft.login.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Account {
    String name;
    String password;
    String ips;
    Timestamp lastAction;
    Integer loginFails;
    Boolean passwordExpired;
    Integer playerType;
    String inviter;

    public Account(String name, String password) {
        this.name = name;
        this.password = password;
        this.ips = "";
        this.lastAction = new Timestamp(System.currentTimeMillis());
        this.loginFails = 0;
        this.passwordExpired = false;
    }

    public Integer getPlayerType() {
        return playerType == null ? 0 : playerType;
    }
}
