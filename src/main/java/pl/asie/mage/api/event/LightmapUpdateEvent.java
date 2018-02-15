/*
 * Copyright (c) 2018 Adrian Siekierka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package pl.asie.mage.api.event;

import net.minecraftforge.fml.common.eventhandler.Event;

public class LightmapUpdateEvent extends Event {
	private final int[] lightmap;
	private final float torchFlicker, partialTicks, bossColorModifier;

	public LightmapUpdateEvent(int[] lightmap, float torchFlicker, float partialTicks, float bossColorModifier) {
		this.lightmap = lightmap;
		this.torchFlicker = torchFlicker;
		this.partialTicks = partialTicks;
		this.bossColorModifier = bossColorModifier;
	}

	public int[] getLightmap() {
		return lightmap;
	}

	public int getLightmapWidth() {
		return 16;
	}

	public int getLightmapHeight() {
		return 16;
	}

	public float getTorchFlicker() {
		return torchFlicker;
	}

	public float getPartialTicks() {
		return partialTicks;
	}

	public float getBossColorModifier() {
		return bossColorModifier;
	}
}
