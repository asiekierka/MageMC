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

import com.elytradev.mini.MiniTransformer;
import com.elytradev.mini.PatchContext;
import com.elytradev.mini.annotation.Patch;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

@Patch.Class("net.minecraft.client.renderer.texture.TextureAtlasSprite")
public class TextureAtlasSpriteTransformer extends MiniTransformer {
	@Patch.Method(
			srg="func_147963_d",
			mcp="generateMipmaps",
			descriptor="(I)V"
	)
	public void patchGenerateMipmaps(PatchContext ctx) {
		ctx.jumpToStart();
		ctx.add(
				new VarInsnNode(Opcodes.ALOAD, 0),
				new MethodInsnNode(
						Opcodes.INVOKESTATIC,
						"pl/asie/mage/hooks/TextureAddedHook", "processTexture", "(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V", false
				)
		);
	}
}
