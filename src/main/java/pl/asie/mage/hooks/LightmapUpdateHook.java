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

package pl.asie.mage.hooks;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;
import pl.asie.mage.MageMod;
import pl.asie.mage.api.event.LightmapUpdateEvent;
import pl.asie.mage.util.Utils;

public final class LightmapUpdateHook {
	private LightmapUpdateHook() {

	}

	public static void onLightmapUpdatePost(int[] lightmap, float torchFlickerX, float bossColorModifier, float bossColorModifierPrev) {
		if (lightmap.length != 256) {
			MageMod.logger.warn("Non-standard lightmap size found: " + lightmap.length);
		}

		float partialTicks = Minecraft.getMinecraft().getRenderPartialTicks();
		MinecraftForge.EVENT_BUS.post(new LightmapUpdateEvent(
				lightmap, torchFlickerX, partialTicks,
				bossColorModifier > 0.0f ? Utils.interpolate(bossColorModifierPrev, bossColorModifier, partialTicks) : 0.0f
		));
	}
}
