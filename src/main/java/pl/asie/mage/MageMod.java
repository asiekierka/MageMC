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

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pl.asie.mage.api.IMagePlugin;
import pl.asie.mage.api.MageApprentice;
import pl.asie.mage.util.colorspace.Colorspaces;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Mod(
		modid = "mage",
		name = "MAGE",
		version = "@VERSION@",
		clientSideOnly = true,
		acceptableRemoteVersions = "*"
)
public class MageMod implements IResourceManagerReloadListener {
	public static Logger logger;
	private static final Set<String> loadedPlugins = new HashSet<>();
	private static final Set<IMagePlugin> pluginSet = new HashSet<>();

	public static boolean isPluginLoaded(String name) {
		return loadedPlugins.contains(name);
	}

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = LogManager.getLogger("mage");
		MinecraftForge.EVENT_BUS.register(ColorPaletteParser.INSTANCE);
		Colorspaces.init();

		String p = event.getSuggestedConfigurationFile().getPath();
		File configPath = new File(p.substring(0, p.lastIndexOf('.')));
		if (!configPath.exists()) {
			configPath.mkdir();
		}

		File mainConfigFile = new File(configPath, "mage.cfg");
		Configuration config = new Configuration(mainConfigFile);

		for (ASMDataTable.ASMData plugin : event.getAsmData().getAll(MageApprentice.class.getName())) {
			String name = (String) plugin.getAnnotationInfo().get("value");
			if (name != null) {
				String description = (String) plugin.getAnnotationInfo().getOrDefault("description", name);
				boolean isDefault = (boolean) plugin.getAnnotationInfo().getOrDefault("isDefault", true);
				if (config.getBoolean(name, "plugins", isDefault, description)) {
					try {
						Object o = Class.forName(plugin.getClassName()).newInstance();
						if (o instanceof IMagePlugin) {
							loadedPlugins.add(name);
							pluginSet.add((IMagePlugin) o);

							IMagePlugin magePlugin = (IMagePlugin) o;
							if (magePlugin.hasConfig()) {
								Configuration c = new Configuration(new File(configPath, name.replaceAll(":", ".") + ".cfg"));
								magePlugin.onConfigReload(c);
								if (c.hasChanged()) {
									c.save();
								}
							}
						} else {
							logger.warn(plugin.getClassName() + " apprentice is not an IMagePlugin!");
						}
					} catch (Throwable t) {
						t.printStackTrace();
					}
				}
			}
		}

		if (config.hasChanged()) {
			config.save();
		}

		for (IMagePlugin plugin : pluginSet) {
			plugin.enable();
		}
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		onResourceManagerReload(Minecraft.getMinecraft().getResourceManager());
		((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager()).registerReloadListener(this);
	}

	@Override
	public void onResourceManagerReload(IResourceManager resourceManager) {
		for (IMagePlugin plugin : pluginSet) {
			plugin.onResourceReload(resourceManager);
		}
	}
}
