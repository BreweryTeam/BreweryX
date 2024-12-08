/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024 The Brewery Team
 *
 * This file is part of BreweryX.
 *
 * BreweryX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreweryX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BreweryX. If not, see <http://www.gnu.org/licenses/gpl-3.0.html>.
 */

package com.dre.brewery;

import com.dre.brewery.configuration.ConfigManager;
import com.dre.brewery.configuration.files.Lang;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
public class Wakeup {

	private static final Lang lang = ConfigManager.getConfig(Lang.class);

	@Getter
	public static List<Wakeup> wakeups = new ArrayList<>();
	public static BreweryPlugin breweryPlugin = BreweryPlugin.getInstance();
	public static int checkId = -1;
	public static Player checkPlayer = null;

	private final Location loc;
	private final UUID id;
	private boolean active = true;

	public Wakeup(Location loc) {
		this.loc = loc;
		this.id = UUID.randomUUID();
	}

	// load from save data
	public Wakeup(Location loc, UUID id) {
		this.loc = loc;
		this.id = id;
	}


	// get the nearest of two random Wakeup-Locations
	public static Location getRandom(Location playerLoc) {
		if (wakeups.isEmpty()) {
			return null;
		}

		List<Wakeup> worldWakes = wakeups.stream()
			.filter(w -> w.active)
			.filter(w -> w.loc.getWorld().equals(playerLoc.getWorld()))
			.collect(Collectors.toList());

		if (worldWakes.isEmpty()) {
			return null;
		}

		Wakeup w1 = calcRandom(worldWakes);
		worldWakes.remove(w1);
		if (w1 == null) return null;

		while (!w1.check()) {
			Logging.errorLog("Please Check Wakeup-Location with id: &6" + wakeups.indexOf(w1));

			w1 = calcRandom(worldWakes);
			if (w1 == null) {
				return null;
			}
			worldWakes.remove(w1);
		}

		Wakeup w2 = calcRandom(worldWakes);
		if (w2 != null) {
			worldWakes.remove(w2);

			while (!w2.check()) {
				Logging.errorLog("Please Check Wakeup-Location with id: &6" + wakeups.indexOf(w2));

				w2 = calcRandom(worldWakes);
				if (w2 == null) {
					return w1.loc;
				}
				worldWakes.remove(w2);
			}


			if (w1.loc.distanceSquared(playerLoc) > w2.loc.distanceSquared(playerLoc)) {
				return w2.loc;
			}
		}
		return w1.loc;
	}

	public static Wakeup calcRandom(List<Wakeup> worldWakes) {
		if (worldWakes.isEmpty()) {
			return null;
		}
		return worldWakes.get((int) Math.round(Math.random() * ((float) worldWakes.size() - 1.0)));
	}

	public static void set(CommandSender sender) {
		if (sender instanceof Player) {

			Player player = (Player) sender;
			wakeups.add(new Wakeup(player.getLocation()));
			lang.sendEntry(sender, "Player_WakeCreated", "" + (wakeups.size() - 1));

		} else {
			lang.sendEntry(sender, "Error_PlayerCommand");
		}
	}

	public static void remove(CommandSender sender, int id) {
		if (wakeups.isEmpty() || id < 0 || id >= wakeups.size()) {
			lang.sendEntry(sender, "Player_WakeNotExist", "" + id);//"&cDer Aufwachpunkt mit der id: &6" + id + " &cexistiert nicht!");
			return;
		}

		Wakeup wakeup = wakeups.get(id);

		if (wakeup.active) {
			wakeup.active = false;
			lang.sendEntry(sender, "Player_WakeDeleted", "" + id);

		} else {
			lang.sendEntry(sender, "Player_WakeAlreadyDeleted", "" + id);
		}
	}

	public static void list(CommandSender sender, int page, String worldOnly) {
		if (wakeups.isEmpty()) {
			lang.sendEntry(sender, "Player_WakeNoPoints");
			return;
		}

		ArrayList<String> locs = new ArrayList<>();
		for (int id = 0; id < wakeups.size(); id++) {

			Wakeup wakeup = wakeups.get(id);

			String s = "&m";
			if (wakeup.active) {
				s = "";
			}

			String world = wakeup.loc.getWorld().getName();

			if (worldOnly == null || world.equalsIgnoreCase(worldOnly)) {
				int x = (int) wakeup.loc.getX();
				int y = (int) wakeup.loc.getY();
				int z = (int) wakeup.loc.getZ();

				locs.add("&6" + s + id + "&f" + s + ": " + world + " " + x + "," + y + "," + z);
			}
		}
		BUtil.list(sender, locs, page);
	}

	public static void check(CommandSender sender, int id, boolean all) {
		if (sender instanceof Player) {
			Player player = (Player) sender;

			if (!all) {
				if (wakeups.isEmpty() || id >= wakeups.size()) {
					lang.sendEntry(sender, "Player_WakeNotExist", "" + id);
					return;
				}

				Wakeup wakeup = wakeups.get(id);
				if (wakeup.check()) {
					player.teleport(wakeup.loc);
				} else {
					String world = wakeup.loc.getWorld().getName();
					int x = (int) wakeup.loc.getX();
					int y = (int) wakeup.loc.getY();
					int z = (int) wakeup.loc.getZ();
					lang.sendEntry(sender, "Player_WakeFilled", "" + id, world, "" + x , "" + y, "" + z);
				}

			} else {
				if (wakeups.isEmpty()) {
					lang.sendEntry(sender, "Player_WakeNoPoints");
					return;
				}
				if (checkPlayer != null && checkPlayer != player) {
					checkId = -1;
				}
				checkPlayer = player;
				tpNext();
			}


		} else {
			lang.sendEntry(sender, "Error_PlayerCommand");
		}
	}

	public boolean check() {
		return (!loc.getBlock().getType().isSolid() && !loc.getBlock().getRelative(0, 1, 0).getType().isSolid());
	}

	public static void tpNext() {
		checkId++;
		if (checkId >= wakeups.size()) {
			lang.sendEntry(checkPlayer, "Player_WakeLast");
			checkId = -1;
			checkPlayer = null;
			return;
		}

		Wakeup wakeup = wakeups.get(checkId);
		if (!wakeup.active) {
			tpNext();
			return;
		}

		String world = wakeup.loc.getWorld().getName();
		int x = (int) wakeup.loc.getX();
		int y = (int) wakeup.loc.getY();
		int z = (int) wakeup.loc.getZ();

		if (wakeup.check()) {
			lang.sendEntry(checkPlayer, "Player_WakeTeleport", checkId, world, "" + x , "" + y, "" + z);
			checkPlayer.teleport(wakeup.loc);
		} else {
			lang.sendEntry(checkPlayer, "Player_WakeFilled", checkId, world, "" + x , "" + y, "" + z);
		}
		lang.sendEntry(checkPlayer, "Player_WakeHint1");
		lang.sendEntry(checkPlayer, "Player_WakeHint2");
	}

	public static void cancel(CommandSender sender) {
		if (checkPlayer != null) {
			checkPlayer = null;
			checkId = -1;
			lang.sendEntry(sender, "Player_WakeCancel");
			return;
		}
		lang.sendEntry(sender, "Player_WakeNoCheck");
	}


	public static void save(ConfigurationSection section, ConfigurationSection oldData) {
		BUtil.createWorldSections(section);

		// loc is saved as a String in world sections with format x/y/z/pitch/yaw
		if (!wakeups.isEmpty()) {

			Iterator<Wakeup> iter = wakeups.iterator();
			for (int id = 0; iter.hasNext(); id++) {
				Wakeup wakeup = iter.next();

				if (!wakeup.active) {
					continue;
				}

				String worldName = wakeup.loc.getWorld().getName();
				String prefix;

				if (worldName.startsWith("DXL_")) {
					prefix = BUtil.getDxlName(worldName) + "." + id;
				} else {
					prefix = wakeup.loc.getWorld().getUID().toString() + "." + id;
				}

				section.set(prefix, wakeup.loc.getX() + "/" + wakeup.loc.getY() + "/" + wakeup.loc.getZ() + "/" + wakeup.loc.getPitch() + "/" + wakeup.loc.getYaw());
			}
		}

		// copy Wakeups that are not loaded
		if (oldData != null){
			for (String uuid : oldData.getKeys(false)) {
				if (!section.contains(uuid)) {
					section.set(uuid, oldData.get(uuid));
				}
			}
		}
	}

	public static void onUnload(World world) {
		wakeups.removeIf(wakeup -> wakeup.loc.getWorld().equals(world));
	}

	public static void unloadWorlds() {
		List<World> worlds = BreweryPlugin.getInstance().getServer().getWorlds();
		wakeups.removeIf(wakeup -> !worlds.contains(wakeup.loc.getWorld()));
	}

}
