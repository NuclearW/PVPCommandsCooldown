package com.nuclearw.pvpcommandscooldown;

public class RemoveFromDisallowTask implements Runnable {
	private String command, playerName;

	public RemoveFromDisallowTask(String command, String playerName) {
		this.command = command;
		this.playerName = playerName;
	}

	@Override
	public void run() {
		PVPCommandsCooldown.removeFromDisabled(command, playerName);
	}
}