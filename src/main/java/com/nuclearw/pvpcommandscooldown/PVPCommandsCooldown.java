package com.nuclearw.pvpcommandscooldown;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class PVPCommandsCooldown extends JavaPlugin implements Listener {
	private static HashMap<String, Long> watchedTimes = new HashMap<String, Long>();
	private static HashMap<String, HashSet<String>> disabledPlayers = new HashMap<String, HashSet<String>>();

	@Override
	public void onEnable() {
		if(!new File(getDataFolder(), "config.yml").exists()) saveDefaultConfig();

		// TODO: Load from config.yml

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
		if(!(dEvent.getDamager() instanceof Player)) return;
		Player attacker = (Player) dEvent.getDamager();

		Iterator<String> i = watchedTimes.keySet().iterator();
		while(i.hasNext()) {
			String command = i.next();

			HashSet<String> disabled = disabledPlayers.get(command);
			if(disabled == null) disabled = new HashSet<String>();
			disabled.add(attacker.getName());

			disabledPlayers.put(command, disabled);
			getServer().getScheduler().scheduleAsyncDelayedTask(this, new RemoveFromDisallowTask(command, attacker.getName()), watchedTimes.get(command) * 20);
		}
	}

	@EventHandler
	public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
		String message = event.getMessage();
		if(!message.startsWith("/")) return;
		String command = message.substring(1).split(" ")[0];
		Player player = event.getPlayer();

		if(disabledPlayers.containsKey(command)) {
			HashSet<String> players = disabledPlayers.get(command);
			if(players == null) players = new HashSet<String>();

			if(players.contains(player.getName())) {
				event.setCancelled(true);
				player.sendMessage("You've attacked another user, you can't use this command right now.");
			}
		}
	}

	public static void removeFromDisabled(String command, String playerName) {
		HashSet<String> disabled = disabledPlayers.get(command);
		if(disabled == null) disabled = new HashSet<String>();
		disabled.remove(playerName);

		disabledPlayers.put(command, disabled);
	}
}