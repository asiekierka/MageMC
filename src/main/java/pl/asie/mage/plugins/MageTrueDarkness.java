/*
 * Copyright (c) 2018 Adrian Siekierka
 *
 * This file is part of MAGE.
 *
 * MAGE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MAGE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MAGE.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.asie.mage.plugins;

import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pl.asie.mage.api.IMagePlugin;
import pl.asie.mage.api.MageApprentice;
import pl.asie.mage.api.event.LightmapUpdateEvent;
import pl.asie.mage.util.colorspace.Colorspace;
import pl.asie.mage.util.colorspace.Colorspaces;

@MageApprentice(value = "mage:trueDarkness", description = "Modifies the existing lightmaps to provide true darkness at night or in dark areas.", isDefault = false, canDisableRuntime = true)
public class MageTrueDarkness implements IMagePlugin {
	public static class FeatureDimensionManager {
		public FeatureDimensionManager(Configuration config, String category, boolean def) {
			enabled = config.getBoolean("enabled", category, def, "Is the feature enabled?");
			String allowedDimensionsStr = config.getString("allowedDimensions", category, "", "Comma-delimited list of dimensions in which the feature is enabled. If empty, no whitelisting is applied!");
			String blockedDimensionsStr = config.getString("blockedDimensions", category, "", "Comma-delimited list of dimensions in which the feature is disabled.");

			applyToSet(allowedDimensions, allowedDimensionsStr);
			applyToSet(blockedDimensions, blockedDimensionsStr);
		}

		private final TIntSet allowedDimensions = new TIntHashSet();
		private final TIntSet blockedDimensions = new TIntHashSet();
		private boolean enabled;

		private void applyToSet(TIntSet set, String ss) {
			set.clear();
			for (String s : ss.split(",")) {
				try {
					set.add(Integer.parseInt(s.trim()));
				} catch (NumberFormatException e) {

				}
			}
		}

		public boolean isEnabled() {
			return enabled;
		}

		public boolean apply(int d) {
			if (!enabled) {
				return false;
			}

			if (!allowedDimensions.isEmpty() && !allowedDimensions.contains(d)) {
				return false;
			} else if (blockedDimensions.contains(d)) {
				return false;
			}

			return true;
		}
	}

	private static final float[] SQRTS;
	private FeatureDimensionManager general, moonPhase;
	private float moonPhaseFactor;

	static {
		SQRTS = new float[16];
		SQRTS[0] = 0;
		for (int i = 1; i < 16; i++) {
			SQRTS[i] = (float) Math.sqrt(i);
		}
	}

	@Override
	public void enable() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public void disable() {
		MinecraftForge.EVENT_BUS.unregister(this);
	}

	@Override
	public boolean hasConfig() {
		return true;
	}

	@Override
	public void onConfigReload(Configuration config) {
		general = new FeatureDimensionManager(config, "general", true);
		moonPhase = new FeatureDimensionManager(config, "moonPhase", false);
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onLightmapUpdate(LightmapUpdateEvent event) {
		// sky=0,block=0 - darkness
		WorldClient world = Minecraft.getMinecraft().world;
		int d = world.provider.getDimension();
		if (!general.apply(d)) {
			return;
		}

		float subtractedBrightness = 1.0f;
		boolean moonPhaseA = moonPhase.apply(d);

		float sunBrightnessAssumed = world.getSunBrightness(1.0f);
		float sunBrightness = MathHelper.clamp((sunBrightnessAssumed - (0.2f * subtractedBrightness)) / (1.0f - (0.2f * subtractedBrightness)), 0.0f, 1.0f);

		// other colors - rescaled to match
		int[] lightmap = event.getLightmap();

		// brightestSky - darkestSky - default light range
		// if sunBrightness == 1.0f, we touch nothing
		// if sunBrightness == 0.0f, we zero everything
		// however, all the values are already premultiplied by sunBrightnessAssumed

		if (Math.abs(sunBrightness - sunBrightnessAssumed) < 1e-4 || sunBrightnessAssumed <= 0.0f) {
			return;
		}

		float brightnessMultiplierStart = sunBrightness / sunBrightnessAssumed;
		float brightnessMultiplierEnd = sunBrightness / sunBrightnessAssumed;
		if (moonPhaseA) {
			brightnessMultiplierEnd = brightnessMultiplierEnd + ((sunBrightnessAssumed - sunBrightness) / sunBrightnessAssumed) * world.getCurrentMoonPhaseFactor();
		}


		for (int block = 0; block < 15; block++) {
			// lmul = brightnessMultiplier..1.0 for block=0..15 inclusive
			// but we want to be slightly more curvaceous about it
			for (int sky = 0; sky < 16; sky++) {
				float brightnessMultiplier = ((brightnessMultiplierStart * (15 - sky)) + (brightnessMultiplierEnd * sky)) / 15f;
				float lmul = brightnessMultiplier + ((1.0f - brightnessMultiplier) * (SQRTS[block] / 4.0f));
				float[] color = Colorspaces.convertFromRGB(lightmap[sky * 16 + block], Colorspace.LAB);
				color[0] *= lmul;
				lightmap[sky * 16 + block] = Colorspaces.convertToRGB(color, Colorspace.LAB) | 0xFF000000;
			}
		}
	}
}
