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

package pl.asie.mage.api_experimental.event;

import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import pl.asie.mage.ColorPaletteParser;

public class ColorPaletteUpdateEvent extends Event {
	private final ColorPaletteParser parser;

	public ColorPaletteUpdateEvent(ColorPaletteParser parser) {
		this.parser = parser;
	}

	public boolean hasColor(String namespace, String color) {
		return parser.hasColor(namespace, color);
	}

	public float[] getColor(String namespace, String color) {
		return parser.getColor(namespace, color);
	}
}
