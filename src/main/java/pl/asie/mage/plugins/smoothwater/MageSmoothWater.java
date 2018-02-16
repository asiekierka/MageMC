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

package pl.asie.mage.plugins.smoothwater;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.BlockStateMapper;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelFluid;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.model.TRSRTransformation;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import pl.asie.mage.api.IMagePlugin;
import pl.asie.mage.api.MageApprentice;

import java.lang.reflect.Field;
import java.util.Collection;

@MageApprentice(value = "mage:smoothWater", description = "Makes vanilla water rendering smoothly lit and fixes under-liquid ambient occlusion.")
public class MageSmoothWater implements IMagePlugin {
	public static boolean isActive = false;
	public static boolean patchModdedFluidAO = false;

	@Override
	public void init() {
		MinecraftForge.EVENT_BUS.register(this);
		isActive = true;
	}

	@Override
	public boolean hasConfig() {
		return true;
	}

	@Override
	public void onConfigReload(Configuration config) {
		patchModdedFluidAO = config.getBoolean("patchModdedFluidAO", "general", true, "Patches default ambient occlusion handling in modded fluids. If false, only does so to vanilla fluids.");
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void onRegisterBlocks(RegistryEvent.Register<Block> event) {
		Field f = ReflectionHelper.findField(Block.class, "blockState", "field_176227_L");
		Field f2 = ReflectionHelper.findField(Block.class, "defaultBlockState", "field_176228_M");

		for (Block b : event.getRegistry()) {
			if (b instanceof BlockLiquidForged) {
				try {
					f.set(b, new ExtendedBlockState(b, b.getBlockState().getProperties().toArray(new IProperty[0]), BlockFluidBase.FLUID_RENDER_PROPS.toArray(new IUnlistedProperty<?>[0])));
					f2.set(b, b.getBlockState().getBaseState().withProperty(BlockLiquid.LEVEL, b.getDefaultState().getValue(BlockLiquid.LEVEL)));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private Fluid getFluid(Block b) {
		if (b instanceof BlockLiquidForged && ((BlockLiquidForged) b).getRenderTypeSuper(b.getDefaultState()) == EnumBlockRenderType.LIQUID) {
			if (b.getMaterial(b.getDefaultState()) == Material.LAVA) {
				return FluidRegistry.LAVA;
			} else if (b.getMaterial(b.getDefaultState()) == Material.WATER) {
				return FluidRegistry.WATER;
			}
		}

		Block lookupBlock = b;
		if (lookupBlock instanceof BlockDynamicLiquid) {
			lookupBlock = BlockDynamicLiquid.getStaticBlock(lookupBlock.getDefaultState().getMaterial());
		}
		return FluidRegistry.lookupFluidForBlock(lookupBlock);
	}

	@SubscribeEvent
	public void onModelRegistry(ModelRegistryEvent event) {
		for (Block b : GameRegistry.findRegistry(Block.class)) {
			if (b instanceof BlockLiquidForged && getFluid(b) != null) {
				ModelLoader.setCustomStateMapper(b, new StateMapperBase() {
					@Override
					protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
						return new ModelResourceLocation(state.getBlock().getRegistryName(), "fluid");
					}
				});
			}
		}
	}

	@SubscribeEvent
	public void onTextureStitch(TextureStitchEvent.Pre event) {
		try {
			Field f1 = ReflectionHelper.findField(Minecraft.class, "modelManager", "field_175617_aL");
			Object one = f1.get(Minecraft.getMinecraft());

			Field f = ReflectionHelper.findField(BlockStateMapper.class, "setBuiltInBlocks", "field_178449_b");
			Collection c = (Collection) f.get(((ModelManager) one).getBlockModelShapes().getBlockStateMapper());
			c.removeIf(o -> o instanceof BlockLiquidForged && getFluid((Block) o) != null);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@SubscribeEvent
	public void onModelBake(ModelBakeEvent event) {
		for (Block b : ForgeRegistries.BLOCKS) {
			if (b instanceof BlockLiquid) {
				Fluid f = getFluid(b);
				if (f != null) {
					ModelFluid fluid = new ModelFluid(f);
					IBakedModel baked = fluid.bake(
							TRSRTransformation.identity(),
							DefaultVertexFormats.ITEM,
							ModelLoader.defaultTextureGetter()
					);
					event.getModelRegistry().putObject(new ModelResourceLocation(b.getRegistryName(), "fluid"), baked);
				}
			}
		}
	}
}
