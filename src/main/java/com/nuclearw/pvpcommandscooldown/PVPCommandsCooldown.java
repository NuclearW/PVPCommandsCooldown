package com.nuclearw.pvpcommandscooldown;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PVPCommandsCooldown extends JavaPlugin implements Listener {
	// Command - Time in seconds to put for cooling down
	private static HashMap<String, Long> watchedTimes = new HashMap<String, Long>();
	// Command - <Player name - Player's last task to remove from disabled>
	private static HashMap<String, HashMap<String, Integer>> disabledPlayers = new HashMap<String, HashMap<String, Integer>>();

	@Override
	public void onEnable() {
		if(!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();

		Set<String> keys = getConfig().getConfigurationSection("Commands").getKeys(false);
		Iterator<String> i = keys.iterator();
		while(i.hasNext()) {
			String key = i.next();
			getLogger().info(key + " " + getConfig().getLong("Commands."+key));
			watchedTimes.put(key, getConfig().getLong("Commands."+key));
		}

		getServer().getPluginManager().registerEvents(this, this);

		getLogger().info("Finished Loading "+getDescription().getFullName());
	}

	@Override
	public void onDisable() {
		getLogger().info("Finished Unloading "+getDescription().getFullName());
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDamageByEntityEvent(EntityDamageEvent event) {
		if(event.isCancelled()) return;
		if(!(event instanceof EntityDamageByEntityEvent)) return;
		EntityDamageByEntityEvent dEvent = (EntityDamageByEntityEvent) event;
		Player attacker;

		if(!(dEvent.getEntity() instanceof Player)) {
			if(dEvent.getEntity() instanceof Projectile) {
				Projectile projectile = (Projectile) dEvent.getEntity();
				if(projectile.getShooter() instanceof Player) {
					attacker = (Player) projectile.getShooter();
				} else {
					return;
				}
			} else if(dEvent.getEntity() instanceof Tameable) {
				Tameable pet = (Tameable) dEvent.getEntity();
				if(pet.isTamed() && pet.getOwner() instanceof Player) {
					attacker = (Player) pet.getOwner();
				} else {
					return;
				}
			} else {
				return;
			}
		} else {
			attacker = (Player) dEvent.getDamager();
		}
		if(!(dEvent.getDamager() instanceof Player)) return;

		Iterator<String> i = watchedTimes.keySet().iterator();
		while(i.hasNext()) {
			String command = i.next();

			HashMap<String, Integer> disabled = disabledPlayers.get(command);
			if(disabled == null) disabled = new HashMap<String, Integer>();

			if(disabled.get(attacker.getName()) != null) {
				getServer().getScheduler().cancelTask(disabled.get(attacker.getName()));
			}

			int id = getServer().getScheduler().scheduleAsyncDelayedTask(this, new RemoveFromDisallowTask(command, attacker.getName()), watchedTimes.get(command) * 20);

			disabled.put(attacker.getName(), id);

			disabledPlayers.put(command, disabled);
		}
	}

	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		String message = event.getMessage();
		if(!message.startsWith("/")) return;
		String command = message.substring(1).split(" ")[0];
		Player player = event.getPlayer();

		if(disabledPlayers.containsKey(command)) {
			HashMap<String, Integer> players = disabledPlayers.get(command);
			if(players == null) players = new HashMap<String, Integer>();

			if(players.containsKey(player.getName())) {
				event.setCancelled(true);
				player.sendMessage("You've attacked another user, you can't use this command right now.");
			}
		}
	}

	public static void removeFromDisabled(String command, String playerName) {
		HashMap<String, Integer> disabled = disabledPlayers.get(command);
		if(disabled == null) disabled = new HashMap<String, Integer>();
		disabled.remove(playerName);

		disabledPlayers.put(command, disabled);
	}
}