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
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.init.MobEffects;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderEnd;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import pl.asie.mage.MageMod;
import pl.asie.mage.api.IMagePlugin;
import pl.asie.mage.api.MageApprentice;
import pl.asie.mage.api.event.LightmapUpdateEvent;
import pl.asie.mage.util.Utils;
import pl.asie.mage.util.colorspace.Colorspace;
import pl.asie.mage.util.colorspace.Colorspaces;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;

@MageApprentice(value = "mage:customLightmap", description = "Adds MCPatcher-compatible custom lightmap support.")
public class MageCustomLightmap implements IMagePlugin {
	private final TIntObjectMap<BufferedImage> images = new TIntObjectHashMap<>();

	private Collection<ResourceLocation> getLocations(World w) {
		int d = w.provider.getDimension();
		ImmutableSet.Builder<ResourceLocation> builder = new ImmutableSet.Builder<>();
		builder.add(
//				new ResourceLocation("mage:lightmap/world" + d + ".png"),
				new ResourceLocation("minecraft:mcpatcher/lightmap/world" + d + ".png") //,
//				new ResourceLocation("mage:lightmap/world.png")
		);

		// heuristics - apply the closest world map we can find
		if (w.provider.isNether() && d != -1) {
			builder.add(new ResourceLocation("minecraft:mcpatcher/lightmap/world-1.png"));
		} else if (w.provider.isSurfaceWorld() && d != 0) {
			builder.add(new ResourceLocation("minecraft:mcpatcher/lightmap/world0.png"));
		} else if (w.provider instanceof WorldProviderEnd && d != 1) {
			builder.add(new ResourceLocation("minecraft:mcpatcher/lightmap/world1.png"));
		}

		return builder.build();
	}

	@Override
	public void init() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public void onResourceReload(IResourceManager manager) {
		images.clear();
	}

	private float pow4(float v) {
		return v*v*v*v;
	}

	private float[] get(float x, int y, BufferedImage image) {
		if (x >= image.getWidth() - 1) {
			return Colorspaces.rgbIntToFloat(image.getRGB(image.getWidth() - 1, y));
		} else if (x <= 0) {
			return Colorspaces.rgbIntToFloat(image.getRGB(0, y));
		} else {
			float[] a = Colorspaces.rgbIntToFloat(image.getRGB((int) x, y));
			float[] b = Colorspaces.rgbIntToFloat(image.getRGB((int) x + 1, y));
			return new float[] {
					Utils.interpolate(a[0], b[0], x % 1),
					Utils.interpolate(a[1], b[1], x % 1),
					Utils.interpolate(a[2], b[2], x % 1)
			};
		}
	}

	private void applyCustomLightmap(LightmapUpdateEvent event, WorldClient world, BufferedImage image) {
		float sunBrightness = world.getSunBrightness(1.0f);
		sunBrightness = MathHelper.clamp((sunBrightness - 0.2f) / 0.8f, 0.0f, 1.0f);

		float gamma = Minecraft.getMinecraft().gameSettings.gammaSetting;
		boolean hasCustomNightVision = image.getHeight() == 64;
		boolean isNightVisionActive = Minecraft.getMinecraft().player.isPotionActive(MobEffects.NIGHT_VISION);

		int yOffset = hasCustomNightVision && isNightVisionActive ? 32 : 0;
		float skyPosX = (sunBrightness * (image.getWidth() - 2));
		if (world.getLastLightningBolt() > 0) {
			skyPosX = (image.getWidth() - 3 + world.getLastLightningBolt());
		}
		float blockPosX = ((event.getTorchFlicker() + 1.0f) * image.getWidth());
		if (blockPosX < 0) blockPosX = 0;
		else if (blockPosX >= image.getWidth()) blockPosX = image.getWidth() - 1;

		float[][] skyColors = new float[16][];
		float[][] blockColors = new float[16][];
		for (int i = 0; i < 16; i++) {
			skyColors[i] = get(skyPosX, yOffset + i, image);
			blockColors[i] = get(blockPosX, yOffset + 16 + i, image);
		}

		for (int i = 0; i < 256; i++) {
			float[] skyColor = skyColors[i >> 4];
			float[] blockColor = blockColors[i & 15];
			float[] color = new float[] {
					Math.min(skyColor[0] + blockColor[0], 1.0f),
					Math.min(skyColor[1] + blockColor[1], 1.0f),
					Math.min(skyColor[2] + blockColor[2], 1.0f)
			};

			if (!hasCustomNightVision && isNightVisionActive) {
				// TODO: Use getNightVisionBrightness
				int ii = Minecraft.getMinecraft().player.getActivePotionEffect(MobEffects.NIGHT_VISION).getDuration();
				float brightness = ii > 200 ? 1.0F : 0.7F + MathHelper.sin(((float)ii - event.getPartialTicks()) * (float)Math.PI * 0.2F) * 0.3F;
				float max = Math.max(color[0], Math.max(color[1], color[2]));
				float multiplier = brightness / max;
				color[0] = color[0] * multiplier;
				color[1] = color[1] * multiplier;
				color[2] = color[2] * multiplier;
			}

			color[0] = color[0] * (1.0f - gamma) + (1.0f - pow4(1.0f - color[0])) * gamma;
			color[1] = color[1] * (1.0f - gamma) + (1.0f - pow4(1.0f - color[1])) * gamma;
			color[2] = color[2] * (1.0f - gamma) + (1.0f - pow4(1.0f - color[2])) * gamma;

			// rgbFloatToInt clamps for us!
			event.getLightmap()[i] = 0xFF000000 | Colorspaces.rgbFloatToInt(color);
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onLightmapUpdate(LightmapUpdateEvent event) {
		WorldClient world = Minecraft.getMinecraft().world;
		int d = world.provider.getDimension();
		if (!images.containsKey(d)) {
			BufferedImage image = null;
			IResource resource = null;

			for (ResourceLocation location : getLocations(world)) {
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
					image = TextureUtil.readBufferedImage(resource.getInputStream());
					if (image.getHeight() != 32 && image.getHeight() != 64) {
						MageMod.logger.warn("Unsupported lightmap height: " + image.getHeight());
						image = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			images.put(d, image);
		}

		BufferedImage image = images.get(d);
		applyCustomLightmap(event, world, image);
	}
}
