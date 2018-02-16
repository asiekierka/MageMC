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
		return new float[] {
				MathHelper.clamp(b.getTemperature(pos), 0.0F, 1.0F),
				MathHelper.clamp(b.getRainfall(), 0.0F, 1.0F)
		};
	}
}
