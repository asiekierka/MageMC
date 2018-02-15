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
import pl.asie.mage.hooks.LightmapUpdateHook;

@Patch.Class("net.minecraft.client.renderer.EntityRenderer")
public class EntityRendererTransformer extends MiniTransformer {
	@Patch.Method(
			srg="func_78472_g",
			mcp="updateLightmap",
			descriptor="(F)V"
	)
	public void patchUpdateLightmap(PatchContext ctx) {
		boolean isObf = !ctx.getMethodName().equals("updateLightmap");
		ctx.jumpToStart();
		ctx.search(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/client/renderer/texture/DynamicTexture", "updateDynamicTexture", "()V", false))
				.jumpBefore();
		ctx.add(
				new VarInsnNode(Opcodes.ALOAD, 0),
				new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/renderer/EntityRenderer", isObf ? "field_78504_Q" : "lightmapColors", "[I"),
				new VarInsnNode(Opcodes.ALOAD, 0),
				new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/renderer/EntityRenderer", isObf ? "field_78514_e" : "torchFlickerX", "F"),
				new VarInsnNode(Opcodes.ALOAD, 0),
				new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/renderer/EntityRenderer", isObf ? "field_82831_U" : "bossColorModifier", "F"),
				new VarInsnNode(Opcodes.ALOAD, 0),
				new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/client/renderer/EntityRenderer", isObf ? "field_82832_V" : "bossColorModifierPrev", "F"),
				new MethodInsnNode(INVOKESTATIC, "pl/asie/mage/hooks/LightmapUpdateHook", "onLightmapUpdatePost", "([IFFF)V", false)
		);
	}
}
