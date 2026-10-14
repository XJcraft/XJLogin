package org.xjcraft.login.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import org.xjcraft.login.velocity.manager.VelocityManagerImpl;

public class VelocityCommandRegister implements SimpleCommand {
    private final VelocityManagerImpl manager;

    public VelocityCommandRegister(VelocityManagerImpl manager) {
        this.manager = manager;
    }

    @Override
    public void execute(Invocation invocation) {
        if (!(invocation.source() instanceof Player)) {
            return;
        }

        Player player = (Player) invocation.source();

        // 只在 login 服务器上生效
        if (!player.getCurrentServer().isPresent()) {
            return;
        }

        String serverName = player.getCurrentServer().get().getServerInfo().getName();
        if (!serverName.equalsIgnoreCase("login")) {
            return;
        }

        manager.register(player, invocation.arguments());
    }
}
