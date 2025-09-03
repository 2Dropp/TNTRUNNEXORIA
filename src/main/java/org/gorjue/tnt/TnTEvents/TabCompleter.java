package org.gorjue.tnt.TnTEvents;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.gorjue.tnt.Main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TabCompleter implements org.bukkit.command.TabCompleter {

    public Main plugin;

    public TabCompleter(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (sender instanceof Player) {
            if (sender.isOp()) {
                if (args.length == 1) {
                    // Completar con las opciones predeterminadas
                    completions.addAll(Arrays.asList("start"));
                }
            }
        }
        return completions;
    }
}