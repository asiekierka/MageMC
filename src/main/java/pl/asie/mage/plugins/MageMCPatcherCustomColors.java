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

import com.google.common.collect.ImmutableSet;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import pl.asie.mage.MageMod;
import pl.asie.mage.api.IMagePlugin;
import pl.asie.mage.api.MageApprentice;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Set;

@MageApprentice(value = "mcpatcher:customColors", description = "Adds MCPatcher-compatible recoloring support for resource packs.")
public class MageMCPatcherCustomColors implements IMagePlugin {
	@FunctionalInterface
	public interface ColorOverride {
		int apply(IBlockState state, IBlockAccess world, BlockPos pos, int tintIndex, BufferedImage image);
	}

	public void override(Set<ResourceLocation> locationSet, Block block, ColorOverride override) {
		IResource resource = null;
		for (ResourceLocation location : locationSet) {
			try {
				resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
			} catch (IOException e) {

			}

			if (resource != null) {
				break;
			}
		}

		if (resource != null) {
			try {
				BufferedImage image = TextureUtil.readBufferedImage(resource.getInputStream());
				Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler((state, worldIn, pos, tintIndex) -> override.apply(state, worldIn, pos, tintIndex, image), block);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onResourceReload(IResourceManager manager) {
		override(ImmutableSet.of(
				new ResourceLocation("mage:colormap/redstone.png"),
				new ResourceLocation("minecraft:mcpatcher/colormap/redstone.png")
		), Blocks.REDSTONE_WIRE, ((state, world, pos, tintIndex, image) -> image.getRGB(state.getValue(BlockRedstoneWire.POWER), 0)));
		override(ImmutableSet.of(
				new ResourceLocation("mage:colormap/pumpkinstem.png"),
				new ResourceLocation("minecraft:mcpatcher/colormap/pumpkinstem.png")
		), Blocks.PUMPKIN_STEM, ((state, world, pos, tintIndex, image) -> image.getRGB(state.getValue(BlockStem.AGE), 0)));
		override(ImmutableSet.of(
				new ResourceLocation("mage:colormap/melonstem.png"),
				new ResourceLocation("minecraft:mcpatcher/colormap/melonstem.png")
		), Blocks.MELON_STEM, ((state, world, pos, tintIndex, image) -> image.getRGB(state.getValue(BlockStem.AGE), 0)));
	}
}
