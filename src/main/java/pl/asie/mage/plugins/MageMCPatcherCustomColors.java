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
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockStem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import pl.asie.mage.MageMod;
import pl.asie.mage.api.IMagePlugin;
import pl.asie.mage.api.MageApprentice;
import pl.asie.mage.api_experimental.event.ColorPaletteUpdateEvent;
import pl.asie.mage.util.colorspace.Colorspaces;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;

@MageApprentice(value = "mcpatcher:customColors", description = "Adds MCPatcher-compatible recoloring support for resource packs.")
public class MageMCPatcherCustomColors implements IMagePlugin {
	private final TObjectIntMap<Potion> originalColors = new TObjectIntHashMap<>();
	private int[] originalColorCode;

	@FunctionalInterface
	public interface ColorOverride {
		int apply(IBlockState state, IBlockAccess world, BlockPos pos, int tintIndex, BufferedImage image);
	}

	@Override
	public void init() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onColorPaletteUpdate(ColorPaletteUpdateEvent event) {
		try {
			Field f = ReflectionHelper.findField(FontRenderer.class, "colorCode", "field_78285_g");
			int[] colorCode = (int[]) f.get(Minecraft.getMinecraft().fontRenderer);

			if (originalColorCode == null) {
				originalColorCode = Arrays.copyOf(colorCode, colorCode.length);
			}

			for (int i = 0; i < 32; i++) {
				String key = Integer.toString(i);
				System.out.println(key);
				if (event.hasColor("minecraft:font_renderer", key)) {
					colorCode[i] = Colorspaces.rgbFloatToInt(event.getColor("minecraft:font_renderer", key));
				} else {
					colorCode[i] = originalColorCode[i];
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		try {
			Field f = ReflectionHelper.findField(Potion.class, "liquidColor", "field_76414_N");

			if (!originalColors.isEmpty()) {
				for (Potion p : Potion.REGISTRY) {
					if (originalColors.containsKey(p)) {
						f.set(p, originalColors.get(p));
					}
				}

				originalColors.clear();
			}

			for (Potion p : Potion.REGISTRY) {
				ResourceLocation loc = p.getRegistryName();
				String name = loc.getResourceDomain().equals("minecraft") ? loc.getResourcePath() : loc.toString();
				if (event.hasColor("minecraft:potion", name)) {
					originalColors.put(p, p.getLiquidColor());
					f.set(p, Colorspaces.rgbFloatToInt(event.getColor("minecraft:potion", name)));
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
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
