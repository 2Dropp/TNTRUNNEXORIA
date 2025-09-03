package org.gorjue.tnt;

import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.gorjue.tnt.Perks.Habilidades;
import org.gorjue.tnt.TnTEvents.PlayerEvents;
import org.gorjue.tnt.TnTEvents.TabCompleter;
import org.gorjue.tnt.TnTEvents.TntCommand;

public class Main extends JavaPlugin {

    private PlayerEvents playerEvents;
    private Habilidades habilidades;

    @Override
    public void onEnable() {
        // Inicializa la clase de eventos y pásale la instancia del plugin
        playerEvents = new PlayerEvents(this);

        // Registra los eventos para que el juego pueda reaccionar a las acciones de los jugadores
        getServer().getPluginManager().registerEvents(playerEvents, this);
        habilidades = new Habilidades(this);
        getServer().getPluginManager().registerEvents(habilidades, this);        // Registra el comando /tnt
        getCommand("tnt").setExecutor(new TntCommand(playerEvents));
        getCommand("tnt").setTabCompleter(new TabCompleter(this));
        initPluginGamerules();
        getLogger().info("TNTTag ha sido activado. ¡Que comience la diversión!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TNTTag ha sido desactivado. ¡Hasta la próxima!");
    }

    public void initPluginGamerules() {
        World world = Bukkit.getWorld("world");
        if (world == null) return;
        world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        world.setDifficulty(Difficulty.HARD);
        world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
    }

}