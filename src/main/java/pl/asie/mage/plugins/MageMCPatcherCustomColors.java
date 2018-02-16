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
import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.init.Blocks;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ColorizerFoliage;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.registries.IRegistryDelegate;
import pl.asie.mage.MageMod;
import pl.asie.mage.api.IMagePlugin;
import pl.asie.mage.api.MageApprentice;
import pl.asie.mage.api_experimental.event.ColorPaletteDataReloadEvent;
import pl.asie.mage.util.BiomeColorUtils;
import pl.asie.mage.util.MethodHandleHelper;
import pl.asie.mage.util.colorspace.Colorspaces;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@MageApprentice(value = "mcpatcher:customColors", description = "Adds partial MCPatcher-compatible recoloring support for resource packs. WIP.")
public class MageMCPatcherCustomColors implements IMagePlugin {
	private final TObjectIntMap<Potion> originalColors = new TObjectIntHashMap<>();
	private final Map<IRegistryDelegate<Block>, IBlockColor> oldColorMap = new HashMap<>();
	private Map<IRegistryDelegate<Block>, IBlockColor> blockColorMap;
	private int[] originalColorCode;

	@FunctionalInterface
	public interface ColorOverride {
		Integer apply(IBlockState state, IBlockAccess world, BlockPos pos, int tintIndex, BufferedImage image);
	}

	@Override
	public void init() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onColorPaletteUpdate(ColorPaletteDataReloadEvent event) {
		if (event.hasAnyColor("minecraft:font_renderer")) {
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
		}

		if (event.hasAnyColor("minecraft:potion")) {
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
	}

	@Nullable
	public BufferedImage getBufferedImage(Set<ResourceLocation> locationSet) {
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
				return image;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return null;
	}

	public boolean override(ColorOverride override, BufferedImage image, Block... blocks) {
		if (blockColorMap != null) {
			for (Block b : blocks) {
				oldColorMap.put(b.delegate, blockColorMap.get(b.delegate));
			}
		}

		Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler((state, worldIn, pos, tintIndex) -> {
			Integer out = override.apply(state, worldIn, pos, tintIndex, image);
			if (out != null) {
				return out;
			} else {
				IBlockColor color = oldColorMap.get(state.getBlock().delegate);
				return color != null ? color.colorMultiplier(state, worldIn, pos, tintIndex) : -1;
			}
		}, blocks);
		return true;
	}

	public boolean override(Set<ResourceLocation> locationSet, ColorOverride override, Block... blocks) {
		BufferedImage image = getBufferedImage(locationSet);
		if (image != null) {
			return override(override, image, blocks);
		} else {
			return false;
		}
	}

	public Integer applyBiome(IBlockState state, IBlockAccess world, BlockPos pos, int tintIndex, BufferedImage image) {
		if (world != null && pos != null) {
			return BiomeColorHelper.getColorAtPos(world, pos, (biome, blockPos) -> {
				float[] coords = BiomeColorUtils.getBiomeImageCoordinates(biome, pos);
				return image.getRGB((int) (coords[0] * image.getWidth()), (int) (coords[1] * image.getHeight()));
			});
		} else {
			return null;
		}
	}

	@Override
	public void onResourceReload(IResourceManager manager) {
		try {
			blockColorMap = (Map<IRegistryDelegate<Block>, IBlockColor>) MethodHandleHelper.findFieldGetter(BlockColors.class, "blockColorMap").invoke(Minecraft.getMinecraft().getBlockColors());
		} catch (Throwable t) {
			MageMod.logger.warn("Could not grab instance of blockColorMap!");
			blockColorMap = null;
		}
		if (blockColorMap != null) {
			for (Map.Entry<IRegistryDelegate<Block>, IBlockColor> entry : oldColorMap.entrySet()) {
				blockColorMap.put(entry.getKey(), entry.getValue());
			}
		}
		oldColorMap.clear();

		override(ImmutableSet.of(
				new ResourceLocation("mage:colormap/redstone.png"),
				new ResourceLocation("minecraft:mcpatcher/colormap/redstone.png")
		), ((state, world, pos, tintIndex, image) -> image.getRGB(state.getValue(BlockRedstoneWire.POWER), 0)), Blocks.REDSTONE_WIRE);
		override(ImmutableSet.of(
				new ResourceLocation("mage:colormap/pumpkinstem.png"),
				new ResourceLocation("minecraft:mcpatcher/colormap/pumpkinstem.png")
		), ((state, world, pos, tintIndex, image) -> image.getRGB(state.getValue(BlockStem.AGE), 0)), Blocks.PUMPKIN_STEM);
		override(ImmutableSet.of(
				new ResourceLocation("mage:colormap/melonstem.png"),
				new ResourceLocation("minecraft:mcpatcher/colormap/melonstem.png")
		), ((state, world, pos, tintIndex, image) -> image.getRGB(state.getValue(BlockStem.AGE), 0)), Blocks.MELON_STEM);

		override(ImmutableSet.of(new ResourceLocation("minecraft:mcpatcher/colormap/water.png")), this::applyBiome, Blocks.WATER, Blocks.FLOWING_WATER);

		// pine/birch
		BufferedImage pineImage = getBufferedImage(ImmutableSet.of(new ResourceLocation("minecraft:mcpatcher/colormap/pine.png")));
		BufferedImage birchImage = getBufferedImage(ImmutableSet.of(new ResourceLocation("minecraft:mcpatcher/colormap/birch.png")));
		if ((pineImage != null || birchImage != null) && blockColorMap != null) {
			override((state, world, pos, tintIndex, image) -> {
				BlockPlanks.EnumType type = state.getValue(BlockOldLeaf.VARIANT);

				if (type == BlockPlanks.EnumType.SPRUCE) {
					image = pineImage;
				} else if (type == BlockPlanks.EnumType.BIRCH) {
					image = birchImage;
				} else {
					return null;
				}

				if (image != null && world != null && pos != null) {
					float[] coords = BiomeColorUtils.getBiomeImageCoordinates(world, pos);
					return image.getRGB((int) (coords[0] * image.getWidth()), (int) (coords[1] * image.getHeight()));
				} else {
					return null;
				}
			}, null, Blocks.LEAVES);
		}
	}
}
