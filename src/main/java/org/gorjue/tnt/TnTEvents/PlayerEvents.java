package org.gorjue.tnt.TnTEvents;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.gorjue.tnt.Perks.Habilidades; // Importa la clase Habilidades

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class PlayerEvents implements Listener {

    private final JavaPlugin plugin;
    private final List<Player> alivePlayers = new ArrayList<>();
    private final List<Player> playersWithBomb = new ArrayList<>();
    private BukkitRunnable countdownTask;
    private boolean gameInProgress = false;
    private int timeLeft = 30;
    private final int MAX_PLAYERS = 50;

    private BossBar bossBar;
    private final Habilidades habilidades;

    // Variables para la nueva funcionalidad
    private final Map<Player, Location> lastLocations = new HashMap<>();
    private final Map<Player, Integer> stillTime = new HashMap<>();
    private BukkitRunnable monitoringTask;

    public PlayerEvents(JavaPlugin plugin) {
        this.plugin = plugin;
        this.bossBar = Bukkit.createBossBar(ChatColor.GOLD + "Jugadores Vivos: 0", BarColor.GREEN, BarStyle.SOLID);
        // Pasa 'plugin' y una referencia a sí mismo para que Habilidades pueda acceder a playersWithBomb
        this.habilidades = new Habilidades(plugin);
    }

    public void iniciarEvento() {
        if (gameInProgress) {
            Bukkit.broadcastMessage(ChatColor.RED + "¡El evento ya está en curso!");
            return;
        }

        alivePlayers.clear();
        playersWithBomb.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
                alivePlayers.add(player);
                clearHotbar(player);
                setSpeed(player, 2);
            }
        }

        if (alivePlayers.size() < 2) {
            Bukkit.broadcastMessage(ChatColor.RED + "¡Se necesitan al menos 2 jugadores para iniciar el juego!");
            gameInProgress = false;
            return;
        }

        gameInProgress = true;
        Collections.shuffle(alivePlayers);

        // Configurar la BossBar
        bossBar.setTitle(ChatColor.YELLOW.toString() + ChatColor.BOLD + "¡TNT Tag en curso!");
        bossBar.setColor(BarColor.GREEN);
        for (Player player : alivePlayers) {
            bossBar.addPlayer(player);
        }
        updateBossBar();
        bossBar.setVisible(true);

        // Iniciar el sistema de perks
        habilidades.iniciarSistemaPerks();
        startMonitoring(); // Iniciar el monitoreo de corredores

        int initialBombs = Math.max(1, alivePlayers.size() / 10);

        Bukkit.broadcastMessage(ChatColor.YELLOW + "¡El evento de TNT Tag ha comenzado!");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "¡" + initialBombs + " jugadores aleatorios tienen la bomba!");

        assignBombs(initialBombs);
        startCountdown();
    }

    private void assignBombs(int numBombs) {
        // Quitamos la bomba de todos los jugadores antes de reasignarlas
        for (Player p : Bukkit.getOnlinePlayers()) {
            removeBombFromPlayer(p); // Llama al método para eliminar la bomba y el glowing
            clearHotbar(p);
            setSpeed(p, 2);
            p.getInventory().setHelmet(null);
        }
        playersWithBomb.clear();

        Collections.shuffle(alivePlayers);
        for (int i = 0; i < numBombs; i++) {
            if (i < alivePlayers.size()) {
                Player p = alivePlayers.get(i);
                giveBombToPlayer(p); // Llama al método para dar la bomba y el glowing
            }
        }
    }

    private void giveBombToPlayer(Player player) {
        if (!playersWithBomb.contains(player)) {
            playersWithBomb.add(player);
            player.getInventory().setHelmet(new ItemStack(Material.TNT));
            setSpeed(player, 3);
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false)); // Le da el efecto de glowing
            Bukkit.broadcastMessage(ChatColor.GOLD + "¡" + player.getName() + " tiene la bomba ahora!");
        }
    }

    private void removeBombFromPlayer(Player player) {
        if (playersWithBomb.contains(player)) {
            playersWithBomb.remove(player);
            player.getInventory().setHelmet(null);
            setSpeed(player, 2);
            player.removePotionEffect(PotionEffectType.GLOWING); // Le quita el efecto de glowing
        }
    }

    private void setSpeed(Player player, int level) {
        player.removePotionEffect(PotionEffectType.SPEED);
        if (level > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, level - 1, false, false));
        }
    }

    private void startCountdown() {
        timeLeft = 30;
        if (countdownTask != null) {
            countdownTask.cancel();
        }
        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft <= 0) {
                    this.cancel();
                    explodeBombs();
                } else {
                    sendActionBars();
                    updateHotbar();
                    playNoteSound();
                    timeLeft--;
                }
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void sendActionBars() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isOnline()) {
                if (playersWithBomb.contains(p)) {
                    String act = ChatColor.RED + "¡Tienes la bomba! " + ChatColor.YELLOW + timeLeft + "s";
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(act));
                } else {
                    String act = ChatColor.YELLOW + "La bomba la tienen: " + ChatColor.GOLD + playersWithBomb.size() + ChatColor.YELLOW + " jugadores | Tiempo: " + ChatColor.GREEN + timeLeft + "s";
                    p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(act));
                }
            }
        }
    }

    private void updateHotbar() {
        Material woolColor = Material.LIME_WOOL;
        if (timeLeft <= 15) {
            woolColor = Material.ORANGE_WOOL;
        }
        if (timeLeft <= 10) {
            woolColor = Material.RED_WOOL;
        }

        ItemStack woolStack = new ItemStack(woolColor, 1);
        for (Player p : playersWithBomb) {
            for (int i = 0; i < 9; i++) {
                p.getInventory().setItem(i, woolStack);
            }
        }
    }

    private void clearHotbar(Player player) {
        for (int i = 0; i < 9; i++) {
            player.getInventory().setItem(i, null);
        }
    }

    private void playNoteSound() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
        }
    }

    private void explodeBombs() {
        List<Player> playersToExplode = new ArrayList<>(playersWithBomb);
        for (Player p : playersToExplode) {
            if (p.isOnline() && alivePlayers.contains(p)) {
                // Modificado para usar una explosión en lugar de setHealth
                Location loc = p.getLocation();
                World world = loc.getWorld();
                world.createExplosion(loc, 4.0f, false, false);
                world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            }
        }
    }

    private void checkGameEnd() {
        if (alivePlayers.size() <= 1) {
            gameInProgress = false;
            if (countdownTask != null) {
                countdownTask.cancel();
            }
            stopMonitoring(); // Detener el monitoreo de corredores

            if (alivePlayers.size() == 1) {
                Player winner = alivePlayers.get(0);

                // Enviar el título y subtítulo a todos los jugadores
                for (Player p : Bukkit.getOnlinePlayers()) {
                    p.sendTitle(
                            ChatColor.GREEN.toString() + ChatColor.BOLD + "GANADOR",
                            ChatColor.YELLOW + "¡" + winner.getName() + " ha ganado!",
                            10,  // Fade in (ticks)
                            70,  // Stay (ticks)
                            20   // Fade out (ticks)
                    );
                }

                Bukkit.broadcastMessage(ChatColor.YELLOW.toString() + ChatColor.BOLD + "¡" + winner.getName() + " HA GANADO!");
            } else {
                Bukkit.broadcastMessage(ChatColor.RED + "¡El juego ha terminado sin un ganador!");
            }

            playersWithBomb.clear();
            alivePlayers.clear();

            for (Player p : Bukkit.getOnlinePlayers()) {
                p.removePotionEffect(PotionEffectType.SPEED);
                p.removePotionEffect(PotionEffectType.GLOWING); // Eliminar el glowing al final
            }

            bossBar.removeAll();
            bossBar.setVisible(false);

            habilidades.detenerSistemaPerks();
        } else {
            int nextBombs = Math.max(1, alivePlayers.size() / 10);
            assignBombs(nextBombs);
            startCountdown();
        }
    }

    private void updateBossBar() {
        int aliveCount = alivePlayers.size();

        double progress = (double) aliveCount / MAX_PLAYERS;
        if (progress > 1.0) progress = 1.0;

        bossBar.setProgress(progress);
        bossBar.setTitle(ChatColor.GOLD + "Jugadores Vivos: " + ChatColor.YELLOW + aliveCount + ChatColor.GOLD + "/" + MAX_PLAYERS);

        if (aliveCount <= 5) {
            bossBar.setColor(BarColor.RED);
        } else if (aliveCount <= 15) {
            bossBar.setColor(BarColor.YELLOW);
        } else {
            bossBar.setColor(BarColor.GREEN);
        }
    }

    public void hideBossBar() {
        if (bossBar != null) {
            bossBar.removeAll();
            bossBar.setVisible(false);
        }
    }

    // Método para iniciar el monitoreo de jugadores quietos
    private void startMonitoring() {
        if (monitoringTask != null) {
            monitoringTask.cancel();
        }

        lastLocations.clear();
        stillTime.clear();

        monitoringTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!gameInProgress) {
                    return;
                }

                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
                        continue;
                    }
                    if (playersWithBomb.contains(player)) {
                        continue;
                    }

                    Location currentLocation = player.getLocation();
                    Location lastLocation = lastLocations.get(player);

                    if (lastLocation == null) {
                        lastLocations.put(player, currentLocation);
                        stillTime.put(player, 0);
                        continue;
                    }

                    if (!currentLocation.getBlock().equals(lastLocation.getBlock())) {
                        stillTime.put(player, 0);
                        lastLocations.put(player, currentLocation);
                    } else {
                        int time = stillTime.getOrDefault(player, 0) + 1;
                        stillTime.put(player, time);

                        if (time >= 15) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 15, 0));
                        }
                    }
                }
            }
        };
        monitoringTask.runTaskTimer(plugin, 0L, 20L);
    }

    // Método para detener el monitoreo de jugadores quietos
    private void stopMonitoring() {
        if (monitoringTask != null) {
            monitoringTask.cancel();
            monitoringTask = null;
        }
        lastLocations.clear();
        stillTime.clear();
    }

    // Método para que otras clases puedan verificar el estado del juego
    public boolean isGameInProgress() {
        return gameInProgress;
    }

    // Método para que otras clases puedan acceder a la lista de jugadores con bomba
    public List<Player> getPlayersWithBomb() {
        return playersWithBomb;
    }

    // --- Event Handlers ---

    @EventHandler
    public void onPlayerDamageByEntity(EntityDamageByEntityEvent event) {
        if (!gameInProgress) return;

        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            Player damager = (Player) event.getDamager();
            Player victim = (Player) event.getEntity();

            if (playersWithBomb.contains(damager) && alivePlayers.contains(victim)) {
                // Llama a onBombPassAttempt de la clase Habilidades
                boolean canPass = habilidades.onBombPassAttempt(damager, victim);

                if (canPass) {
                    removeBombFromPlayer(damager);
                    clearHotbar(damager);
                    giveBombToPlayer(victim);
                } else {
                    // Cancela el evento de daño si el pase fue bloqueado
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!gameInProgress) return;
        Player player = event.getEntity();
        event.setDeathMessage(null);

        if (playersWithBomb.contains(player)) {
            Location loc = player.getLocation();
            World world = loc.getWorld();
            world.createExplosion(loc, 4.0f, false, false);
            world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            Bukkit.broadcastMessage(ChatColor.DARK_RED.toString() + ChatColor.BOLD + "¡" + player.getName() + " HA EXPLOTADO!");
        }

        removeBombFromPlayer(player);
        clearHotbar(player);
        alivePlayers.remove(player);
        bossBar.removePlayer(player);

        updateBossBar();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.setGameMode(GameMode.SPECTATOR);
            player.removePotionEffect(PotionEffectType.SPEED);
            player.removePotionEffect(PotionEffectType.GLOWING);
        }, 1L);

        checkGameEnd();
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!gameInProgress) return;
        Player player = event.getPlayer();

        if (alivePlayers.contains(player)) {
            alivePlayers.remove(player);
            removeBombFromPlayer(player);
            bossBar.removePlayer(player);
            updateBossBar();
        }
        checkGameEnd();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        event.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SATURATION, 999999999, 2));
        if (gameInProgress) {
            event.getPlayer().sendMessage(ChatColor.RED + "Hay un juego de TNT Tag en curso. Por favor, espera a que termine.");
            event.getPlayer().setGameMode(GameMode.SPECTATOR);
            bossBar.addPlayer(event.getPlayer());
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (gameInProgress) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryDropItem(PlayerDropItemEvent event) {
        if (gameInProgress) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        event.setCancelled(true);
    }
}