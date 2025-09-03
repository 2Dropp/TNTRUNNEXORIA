package org.gorjue.tnt.Perks;

import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Habilidades implements Listener {

    private final JavaPlugin plugin;
    private final List<Location> goldBlocks = new ArrayList<>();
    private final Random random = new Random();
    private BukkitRunnable perkSpawnTask;

    // IDs de los perks para identificarlos
    private final String MEGA_SALTO_ID = "mega_salto";
    private final String VELOCIDAD_RELAMPAGO_ID = "velocidad_relampago";
    private final String CASCO_ANTIEXPLOSIONES_ID = "casco_antiexplosiones";

    private final List<Player> antiExplosionPlayers = new ArrayList<>();

    public Habilidades(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void iniciarSistemaPerks() {
        findGoldBlocks();
        if (goldBlocks.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "No se encontraron bloques de oro para generar perks.");
            return;
        }
        spawnAllPermanentArmorStands();
        startPerkSpawnTask();
    }

    public void detenerSistemaPerks() {
        if (perkSpawnTask != null) {
            perkSpawnTask.cancel();
        }
        // Limpiar Armor Stands y items de perks restantes
        for (World world : Bukkit.getWorlds()) {
            world.getEntitiesByClass(ArmorStand.class).stream()
                    .filter(as -> as.getCustomName() != null && as.getCustomName().contains("HABILIDAD EN:"))
                    .forEach(org.bukkit.entity.Entity::remove);
            world.getEntitiesByClass(Item.class).stream()
                    .filter(item -> item.getItemStack().hasItemMeta() && item.getItemStack().getItemMeta().hasDisplayName() &&
                            (item.getItemStack().getItemMeta().getDisplayName().contains(ChatColor.GOLD + "Mega Salto") ||
                                    item.getItemStack().getItemMeta().getDisplayName().contains(ChatColor.YELLOW + "Velocidad Relámpago") ||
                                    item.getItemStack().getItemMeta().getDisplayName().contains(ChatColor.AQUA + "Casco Antiexplosiones")))
                    .forEach(org.bukkit.entity.Entity::remove);
        }
        antiExplosionPlayers.clear();
    }

    private void findGoldBlocks() {
        goldBlocks.clear();
        for (World world : Bukkit.getWorlds()) {
            for (int x = -100; x <= 100; x++) {
                for (int y = 0; y <= 256; y++) {
                    for (int z = -100; z <= 100; z++) {
                        Location loc = new Location(world, x, y, z);
                        if (loc.getBlock().getType() == Material.GOLD_BLOCK) {
                            goldBlocks.add(loc);
                        }
                    }
                }
            }
        }
    }

    private void spawnAllPermanentArmorStands() {
        // Limpiar los anteriores para evitar sobrecarga al reiniciar
        for (World world : Bukkit.getWorlds()) {
            world.getEntitiesByClass(ArmorStand.class).stream()
                    .filter(as -> as.getCustomName() != null && as.getCustomName().contains("HABILIDAD EN:"))
                    .forEach(org.bukkit.entity.Entity::remove);
        }

        for (Location goldBlockLoc : goldBlocks) {
            Location spawnLoc = goldBlockLoc.clone().add(0.5, 1.5, 0.5);
            ArmorStand stand = spawnLoc.getWorld().spawn(spawnLoc, ArmorStand.class);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setArms(false);
            stand.setSmall(true);
            stand.setVisible(false);
            stand.setCustomName(ChatColor.YELLOW + "HABILIDAD EN: " + 15 + " segundos");
            stand.setCustomNameVisible(true);
            stand.setHeadPose(new EulerAngle(0, Math.toRadians(random.nextInt(360)), 0));

            // Tarea para la rotación del Armor Stand
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!stand.isDead() && stand.isValid()) {
                        stand.setHeadPose(stand.getHeadPose().add(0, Math.toRadians(5), 0));
                    } else {
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 0, 1L);
        }
    }

    private void startPerkSpawnTask() {
        perkSpawnTask = new BukkitRunnable() {
            int time = 15;
            @Override
            public void run() {
                if (time > 0) {
                    // Actualizar el nombre de los armor stands
                    for (World world : Bukkit.getWorlds()) {
                        world.getEntitiesByClass(ArmorStand.class).stream()
                                .filter(as -> as.getCustomName() != null && as.getCustomName().contains("HABILIDAD EN:"))
                                .forEach(as -> as.setCustomName(ChatColor.YELLOW + "HABILIDAD EN: " + time + " segundos"));
                    }
                    time--;
                } else {
                    spawnAllPerkItems(); // Llamada al nuevo método
                    time = 15;
                }
            }
        };
        perkSpawnTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void spawnAllPerkItems() {
        if (goldBlocks.isEmpty()) return;

        // Limpiar ítems anteriores antes de generar nuevos
        for (World world : Bukkit.getWorlds()) {
            world.getEntitiesByClass(Item.class).stream()
                    .filter(item -> item.getItemStack().hasItemMeta() && item.getItemStack().getItemMeta().hasDisplayName() &&
                            (item.getItemStack().getItemMeta().getDisplayName().contains(ChatColor.GOLD + "Mega Salto") ||
                                    item.getItemStack().getItemMeta().getDisplayName().contains(ChatColor.YELLOW + "Velocidad Relámpago") ||
                                    item.getItemStack().getItemMeta().getDisplayName().contains(ChatColor.AQUA + "Casco Antiexplosiones")))
                    .forEach(org.bukkit.entity.Entity::remove);
        }

        for (Location goldBlockLoc : goldBlocks) {
            Location itemSpawnLoc = goldBlockLoc.clone().add(0.5, 1.0, 0.5);

            ItemStack perkItem;
            int chance = random.nextInt(100);

            if (chance < 25) {
                perkItem = new ItemStack(Material.TURTLE_HELMET);
                ItemMeta meta = perkItem.getItemMeta();
                meta.setDisplayName(ChatColor.AQUA + "Casco Antiexplosiones");
                perkItem.setItemMeta(meta);
            } else if (chance < 50) {
                perkItem = new ItemStack(Material.FEATHER); // Objeto visual para Velocidad Relámpago
                ItemMeta meta = perkItem.getItemMeta();
                meta.setDisplayName(ChatColor.YELLOW + "Velocidad Relámpago");
                perkItem.setItemMeta(meta);
            } else {
                perkItem = new ItemStack(Material.SLIME_BALL); // Objeto visual para Mega Salto
                ItemMeta meta = perkItem.getItemMeta();
                meta.setDisplayName(ChatColor.GOLD + "Mega Salto");
                perkItem.setItemMeta(meta);
            }

            Item droppedItem = itemSpawnLoc.getWorld().dropItem(itemSpawnLoc, perkItem);
            droppedItem.setPickupDelay(0);
            droppedItem.setGravity(true);
            droppedItem.setCustomNameVisible(true);
            droppedItem.setCustomName(perkItem.getItemMeta().getDisplayName());
        }
    }

    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        String displayName = event.getItem().getItemStack().getItemMeta().getDisplayName();

        if (displayName != null && (displayName.contains("Mega Salto") || displayName.contains("Velocidad Relámpago") || displayName.contains("Casco Antiexplosiones"))) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            if (displayName.contains(ChatColor.GOLD + "Mega Salto")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 5, 2));
                player.playSound(player.getLocation(), Sound.ENTITY_HORSE_JUMP, 1.0f, 1.0f);
            } else if (displayName.contains(ChatColor.YELLOW + "Velocidad Relámpago")) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, 3));
                player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1.0f, 1.0f);
            } else if (displayName.contains(ChatColor.AQUA + "Casco Antiexplosiones")) {
                antiExplosionPlayers.add(player);
                player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.0f);
                player.sendMessage(ChatColor.AQUA + "¡Tu casco antiexplosiones te protegerá por 5 segundos!");

                // Añadir el efecto de suerte
                player.addPotionEffect(new PotionEffect(PotionEffectType.LUCK, 20 * 5, 1));

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        antiExplosionPlayers.remove(player);
                        player.sendMessage(ChatColor.RED + "Tu casco antiexplosiones ha caducado.");
                    }
                }.runTaskLater(plugin, 20 * 5);
            }

            event.getItem().remove();
        }
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
                    event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
                if (antiExplosionPlayers.contains(player)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    // Nuevo método para manejar el intento de pasar la bomba.
    // Debes llamar a este método desde la clase donde se pasa la bomba.
    public boolean onBombPassAttempt(Player giver, Player receiver) {
        // Comprobar si el jugador que recibe tiene el efecto de Luck
        if (receiver.hasPotionEffect(PotionEffectType.LUCK)) {
            // Mandar mensaje a ambos jugadores
            giver.sendMessage(ChatColor.RED + "¡" + receiver.getName() + " está protegido por el casco!, ¡no le puedes pasar la bomba!");
            receiver.sendMessage(ChatColor.AQUA + "¡Tu casco antiexplosiones te ha protegido de recibir la bomba de " + giver.getName() + "!");
            return false; // Retorna falso para indicar que el pase fue bloqueado
        }
        return true; // Retorna verdadero si el pase puede proceder
    }
}