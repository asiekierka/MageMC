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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SmoothWaterTransformer implements IClassTransformer {
	public static void appendIsTranslucentPatch(ClassNode cn, String methodName, String transformedName) {
		MethodNode methodNode = new MethodNode(Opcodes.ACC_PUBLIC, methodName, "(Lnet/minecraft/block/state/IBlockState;)Z", null, null);
		LabelNode l0 = new LabelNode(new Label());
		methodNode.instructions.add(l0);
		methodNode.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "pl/asie/mage/plugins/smoothwater/MageSmoothWater", "isActive", "Z"));
		LabelNode l1 = new LabelNode(new Label());
		methodNode.instructions.add(new JumpInsnNode(Opcodes.IFNE, l1));
		methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		methodNode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
		methodNode.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/block/Block", methodName, "(Lnet/minecraft/block/state/IBlockState;)Z", false));
		LabelNode l2 = new LabelNode(new Label());
		methodNode.instructions.add(new JumpInsnNode(Opcodes.IFEQ, l2));
		methodNode.instructions.add(l1);
		methodNode.instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
		methodNode.instructions.add(new InsnNode(Opcodes.ICONST_1));
		LabelNode l3 = new LabelNode(new Label());
		methodNode.instructions.add(new JumpInsnNode(Opcodes.GOTO, l3));
		methodNode.instructions.add(l2);
		methodNode.instructions.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
		methodNode.instructions.add(new InsnNode(Opcodes.ICONST_0));
		methodNode.instructions.add(l3);
		methodNode.instructions.add(new FrameNode(Opcodes.F_SAME1, 0, null, 1, new Object[]{Opcodes.INTEGER}));
		methodNode.instructions.add(new InsnNode(Opcodes.IRETURN));
		LabelNode l4 = new LabelNode(new Label());
		methodNode.instructions.add(l4);
		methodNode.maxLocals = 2;
		methodNode.maxStack = 2;
		cn.methods.add(methodNode);
	}

	public static class MV extends MethodVisitor {
		public MV(int api, MethodVisitor parent) {
			super(api, parent);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name,
		                            String desc, boolean itf) {
			if ("<init>".equals(name) && "net/minecraft/block/BlockLiquid".equals(owner)) {
				super.visitMethodInsn(opcode, "pl/asie/mage/plugins/smoothwater/BlockLiquidForged", name, desc, itf);
			} else {
				super.visitMethodInsn(opcode, owner, name, desc, itf);
			}
		}
	}

	public static class CV extends ClassVisitor {
		public CV(int api, ClassVisitor cv) {
			super(api, cv);
		}

		@Override
		public void visit(int version, int access, String name, String signature,
		                  String superName, String[] interfaces) {
			super.visit(version, access, name, signature, "pl/asie/mage/plugins/smoothwater/BlockLiquidForged", interfaces);
		}

		@Override
		public MethodVisitor visitMethod(int access, String name, String desc,
		                                 String signature, String[] exceptions) {
			MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
			return "<init>".equals(name) ? new MV(Opcodes.ASM5, parent) : parent;
		}
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null)
			return null;

		ClassReader reader = new ClassReader(basicClass);
		if ("net/minecraft/block/BlockLiquid".equals(reader.getSuperName())) {
			System.out.println("[SmoothWaterTransformer] Patched " + transformedName + "!");

			ClassWriter writer = new ClassWriter(0);
			ClassVisitor visitor = new CV(Opcodes.ASM5, writer);

			reader.accept(visitor, 0);
			return writer.toByteArray();
		} else if ("net/minecraftforge/fluids/BlockFluidBase".equals(transformedName)) {
			System.out.println("[SmoothWaterTransformer] Patched " + transformedName + "!");

			ClassNode node = new ClassNode(Opcodes.ASM5);
			reader.accept(node, 0);

			boolean isObf = false;

		    for (MethodNode methodNode : node.methods) {
		    	if ("func_180661_e".equals(methodNode.name)) {
		    		isObf = true;
		    		break;
			    }
		    }

			appendIsTranslucentPatch(node, isObf ? "func_149751_l" : "isTranslucent", transformedName);

			ClassWriter writer = new ClassWriter(0);
			node.accept(writer);
			return writer.toByteArray();
		} else {
			return basicClass;
		}
	}
}
