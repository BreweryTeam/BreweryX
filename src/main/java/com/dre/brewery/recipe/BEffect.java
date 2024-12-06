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

package com.dre.brewery.recipe;

import com.dre.brewery.BreweryPlugin;
import com.dre.brewery.utility.BUtil;
import com.dre.brewery.utility.Logging;
import com.dre.brewery.utility.MinecraftVersion;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class BEffect implements Cloneable {

	private final PotionEffectType type;
	private short minlvl;
	private short maxlvl;
	private short minduration;
	private short maxduration;
	private boolean hidden = false;


	public BEffect(PotionEffectType type, short minlvl, short maxlvl, short minduration, short maxduration, boolean hidden) {
		this.type = type;
		this.minlvl = minlvl;
		this.maxlvl = maxlvl;
		this.minduration = minduration;
		this.maxduration = maxduration;
		this.hidden = hidden;
	}

	public BEffect(String effectString) {
		String[] effectSplit = effectString.split("/");
		String effect = effectSplit[0];
		if (effect.equalsIgnoreCase("WEAKNESS") ||
				effect.equalsIgnoreCase("INCREASE_DAMAGE") ||
				effect.equalsIgnoreCase("SLOW") ||
				effect.equalsIgnoreCase("SPEED") ||
				effect.equalsIgnoreCase("REGENERATION")) {
			// hide these effects as they put crap into lore
			// Dont write Regeneration into Lore, its already there storing data!
			hidden = true;
		} else if (effect.endsWith("X")) {
			hidden = true;
			effect = effect.substring(0, effect.length() - 1);
		}
		type = PotionEffectType.getByName(effect);
		if (type == null) {
			Logging.errorLog("Effect: " + effect + " does not exist!");
			return;
		}

		if (effectSplit.length == 3) {
			String[] range = effectSplit[1].split("-");
			if (type.isInstant()) {
				setLvl(range);
			} else {
				setLvl(range);
				range = effectSplit[2].split("-");
				setDuration(range);
			}
		} else if (effectSplit.length == 2) {
			String[] range = effectSplit[1].split("-");
			if (type.isInstant()) {
				setLvl(range);
			} else {
				setDuration(range);
				maxlvl = 3;
				minlvl = 1;
			}
		} else {
			maxduration = 20;
			minduration = 10;
			maxlvl = 3;
			minlvl = 1;
		}
	}

	private void setLvl(String[] range) {
		if (range.length == 1) {
			maxlvl = (short) BUtil.parseInt(range[0]);
			minlvl = 1;
		} else {
			maxlvl = (short) BUtil.parseInt(range[1]);
			minlvl = (short) BUtil.parseInt(range[0]);
		}
	}

	private void setDuration(String[] range) {
		if (range.length == 1) {
			maxduration = (short) BUtil.parseInt(range[0]);
			minduration = (short) (maxduration / 8);
		} else {
			maxduration = (short) BUtil.parseInt(range[1]);
			minduration = (short) BUtil.parseInt(range[0]);
		}
	}

	public PotionEffect generateEffect(int quality) {
		int duration = calcDuration(quality);
		int lvl = calcLvl(quality);

		if (lvl < 1 || (duration < 1 && !type.isInstant())) {
			return null;
		}

		duration *= 20;
		if (BreweryPlugin.getMCVersion().isOrEarlier(MinecraftVersion.V1_14)) {
			@SuppressWarnings("deprecation")
			double modifier = type.getDurationModifier();
			duration /= modifier;
		}
		return type.createEffect(duration, lvl - 1);
	}

	public void apply(int quality, Player player) {
		PotionEffect effect = generateEffect(quality);
		if (effect != null) {
			BUtil.reapplyPotionEffect(player, effect, true);
		}
	}

	public int calcDuration(float quality) {
		return (int) Math.round(minduration + ((maxduration - minduration) * (quality / 10.0)));
	}

	public int calcLvl(float quality) {
		return (int) Math.round(minlvl + ((maxlvl - minlvl) * (quality / 10.0)));
	}

	public void writeInto(PotionMeta meta, int quality) {
		if ((calcDuration(quality) > 0 || type.isInstant()) && calcLvl(quality) > 0) {
			meta.addCustomEffect(type.createEffect(0, 0), true);
		} else {
			meta.removeCustomEffect(type);
		}
	}

	public boolean isValid() {
		return type != null && minlvl >= 0 && maxlvl >= 0 && minduration >= 0 && maxduration >= 0;
	}

	public boolean isHidden() {
		return hidden;
	}

	public PotionEffectType getType() {
		return type;
	}

	@Override
	public String toString() {
		return type.getName() + "/" + minlvl + "-" + maxlvl + "/" + minduration + "-" + maxduration;
	}

    @Override
    public BEffect clone() {
        try {
            BEffect clone = (BEffect) super.clone();
            clone.minlvl = minlvl;
			clone.maxlvl = maxlvl;
			clone.minduration = minduration;
			clone.maxduration = maxduration;
			clone.hidden = hidden;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
