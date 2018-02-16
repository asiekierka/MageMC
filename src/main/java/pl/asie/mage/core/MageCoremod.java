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

package pl.asie.mage.core;

import com.elytradev.mini.MiniCoremod;
import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.TransformerExclusions({"com.elytradev.mini", "pl.asie.mage.core", "pl.asie.mage.plugins.smoothwater"})
@IFMLLoadingPlugin.SortingIndex(1001)
public class MageCoremod extends MiniCoremod {
	public MageCoremod() {
		super(EntityRendererTransformer.class, SmoothWaterTransformer.class);
	}

	@Override
	public String getAccessTransformerClass() {
		return "pl.asie.mage.core.MageAT";
	}
}
