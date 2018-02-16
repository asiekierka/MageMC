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

package pl.asie.mage.util;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColorHelper;

public final class BiomeColorUtils {
	private BiomeColorUtils() {

	}
	public static float[] getBiomeImageCoordinates(IBlockAccess access, BlockPos pos) {
		return getBiomeImageCoordinates(access.getBiome(pos), pos);
	}

	public static float[] getBiomeImageCoordinates(Biome b, BlockPos pos) {
		float temperature = MathHelper.clamp(b.getTemperature(pos), 0.0f, 1.0f);
		float humidity = MathHelper.clamp(b.getRainfall(), 0.0f, 1.0f);
		humidity = humidity * temperature;
		return new float[] {
				MathHelper.clamp(1.0f - temperature, 0.0F, 1.0F),
				MathHelper.clamp(1.0f - humidity, 0.0F, 1.0F)
		};
	}
}
