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

package pl.asie.mage;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.io.IOUtils;
import pl.asie.mage.api_experimental.event.ColorPaletteDataReloadEvent;
import pl.asie.mage.util.colorspace.Colorspaces;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public final class ColorPaletteParser {
	public static final ColorPaletteParser INSTANCE = new ColorPaletteParser();
	private final Gson gson = new Gson();
	private final ResourceLocation COLOR_PALETTE_LOC = new ResourceLocation("mage", "color_palette.json");
	private final ResourceLocation COLOR_PALETTE_MCPATCHER_LOC = new ResourceLocation("minecraft", "mcpatcher/color.properties");
	private Data data;

	private static class Data {
		public int version;
		public Map<String, Map<String, float[]>> palettes;

		public void add(String namespace, String key, int value) {
			float[] rgb = Colorspaces.rgbIntToFloat(value);
			add(namespace, key, new float[] { rgb[0], rgb[1], rgb[2], 1.0f });
		}

		public void add(String namespace, String key, float[] value) {
			if (!palettes.containsKey(namespace)) {
				palettes.put(namespace, new HashMap<>());
			}
			palettes.get(namespace).put(key, value);
		}

		public void add(Data other) {
			for (String s : other.palettes.keySet()) {
				if (!palettes.containsKey(s)) {
					palettes.put(s, other.palettes.get(s));
				} else {
					Map<String, float[]> parent = palettes.get(s);
					for (Map.Entry<String, float[]> entry : other.palettes.get(s).entrySet()) {
						parent.put(entry.getKey(), entry.getValue());
					}
				}
			}
		}
	}

	private ColorPaletteParser() {

	}

	public boolean hasAnyColor(String namespace) {
		return data.palettes.containsKey(namespace) && !data.palettes.get(namespace).isEmpty();
	}

	public boolean hasColor(String namespace, String color) {
		return data.palettes.containsKey(namespace) && data.palettes.get(namespace).containsKey(color);
	}

	public float[] getColor(String namespace, String color) {
		if (hasColor(namespace, color)) {
			return data.palettes.get(namespace).get(color);
		} else {
			return new float[] { 0.0f, 0.0f, 0.0f, 0.0f }; // Fallback
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onTextureStitchPre(TextureStitchEvent.Pre event) {
		this.data = new Data();
		this.data.version = 1;
		this.data.palettes = new HashMap<>();

		try {
			for (IResource resource : Minecraft.getMinecraft().getResourceManager().getAllResources(COLOR_PALETTE_MCPATCHER_LOC)) {
				String[] strings = IOUtils.toString(resource.getInputStream(), Charsets.UTF_8).split("\n");
				for (String s : strings) {
					if (!s.contains("=")) {
						MageMod.logger.warn("Unsupported MCPatcher color line: " + s);
						continue;
					}
					String key = s.substring(0, s.indexOf("=")).trim();
					String value = s.substring(s.indexOf("=") + 1).trim();
					int valueRGB = Integer.parseInt(value, 16);
					if (key.startsWith("potion.")) {
						String potionName = key.substring(7);
						String oldPotionName = potionName;
						this.data.add("minecraft:potion", potionName, valueRGB);
						// reformat to post-1.11 name format
						for (int i = 0; i < potionName.length(); i++) {
							char c = potionName.charAt(i);
							if (c >= 65 && c <= 90) {
								String p = potionName.substring(0, i) + "_" + potionName.substring(i, i + 1).toLowerCase();
								potionName = i == potionName.length() - 1 ? p : p + potionName.substring(i + 1);
								i = i + 1;
							}
						}
						if (!potionName.equals(oldPotionName)) {
							this.data.add("minecraft:potion", potionName, valueRGB);
						}
					} else if (key.startsWith("text.code.")) {
						this.data.add("minecraft:font_renderer", key.substring(10), valueRGB);
					} else if (key.equals("lilypad")) {
						this.data.add("minecraft:lilypad", "lilypad", valueRGB);
					} else {
						MageMod.logger.warn("Unsupported MCPatcher key: " + key);
					}
				}
			}
		} catch (FileNotFoundException e) {
			// pass
		} catch (IOException e) {
			e.printStackTrace();
		}


		try {
			for (IResource resource : Minecraft.getMinecraft().getResourceManager().getAllResources(COLOR_PALETTE_LOC)) {
				Data data = gson.fromJson(new InputStreamReader(resource.getInputStream()), Data.class);
				if (data.version != 1) {
					MageMod.logger.warn("Unsupported version of color_palette.json found in " + resource.getResourcePackName() + " - not loaded.");
					continue;
				}
				this.data.add(data);
			}
		} catch (FileNotFoundException e) {
			// pass
		} catch (IOException e) {
			e.printStackTrace();
		}

		MinecraftForge.EVENT_BUS.post(new ColorPaletteDataReloadEvent(this));
	}
}
