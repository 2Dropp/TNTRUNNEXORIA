package org.gorjue.tnt.TnTEvents;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.gorjue.tnt.TnTEvents.PlayerEvents;

public class TntCommand implements CommandExecutor {

    private final PlayerEvents playerEvents;

    public TntCommand(PlayerEvents playerEvents) {
        this.playerEvents = playerEvents;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.isOp()) {
            if (args.length == 1 && args[0].equalsIgnoreCase("start")) {
                playerEvents.iniciarEvento();
                return true;
            }
            sender.sendMessage("§cUso: /tnt start");
            return false;
        }
        return true;
    }
}
