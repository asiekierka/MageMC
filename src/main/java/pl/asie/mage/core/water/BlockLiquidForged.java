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

package pl.asie.mage.core.water;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.IFluidBlock;
import pl.asie.mage.plugins.MageSmoothWater;

import javax.annotation.Nonnull;

public abstract class BlockLiquidForged extends BlockLiquid {
	protected BlockLiquidForged(Material materialIn) {
		super(materialIn);
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return MageSmoothWater.isActive ? EnumBlockRenderType.MODEL : super.getRenderType(state);
	}

	public EnumBlockRenderType getRenderTypeSuper(IBlockState state) {
		return super.getRenderType(state);
	}

	@Override
	public boolean isTranslucent(IBlockState state) {
		return MageSmoothWater.isActive || super.isTranslucent(state);
	}

	@Override
	public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
		if (getRenderType(blockState) == EnumBlockRenderType.MODEL) {
			IBlockState neighbor = blockAccess.getBlockState(pos.offset(side));
			if (neighbor.getMaterial() == blockState.getMaterial()) {
				return false;
			}
			if (side == EnumFacing.UP) {
				return true;
			}
		}
		return super.shouldSideBeRendered(blockState, blockAccess, pos, side);
	}

	private float getFluidHeightForRender(IBlockAccess world, BlockPos pos, @Nonnull IBlockState up) {
		IBlockState here = world.getBlockState(pos);
		if (here.getMaterial().isLiquid() && here.getMaterial() == up.getMaterial()) {
			return 1;
		}

		if (here.getBlock() == this && here.getBlock().getMetaFromState(here) == 0) {
			return 0.875f;
		}

		if (here.getBlock() instanceof BlockLiquid) {
			return Math.min(1 - BlockLiquid.getLiquidHeightPercent(here.getValue(BlockLiquid.LEVEL)), 14f / 16);
		}

     	return (here.getBlock().isAir(here, world, pos) ? 0 : (-7/8f));
	}

	private float getFluidHeightAverage(float... flow) {
		float total = 0;
		int count = 0;

		float end = 0;

		for (int i = 0; i < flow.length; i++)
		{
			if (flow[i] >= 14f / 16)
			{
				total += flow[i] * 10;
				count += 10;
			}

			if (flow[i] >= 0)
			{
				total += flow[i];
				count++;
			}
		}

		if (end == 0)
			end = total / count;

		return end;
	}

	private double getFlowDirection(IBlockAccess world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		if (!state.getMaterial().isLiquid()) {
			return -1000.0;
		}
		Vec3d vec = getFlow(world, pos, state);
		return vec.x == 0.0D && vec.z == 0.0D ? -1000.0D : Math.atan2(vec.z, vec.x) - Math.PI / 2D;
	}

	@Override
	@Nonnull
	public IBlockState getExtendedState(@Nonnull IBlockState oldState, @Nonnull IBlockAccess worldIn, @Nonnull BlockPos pos) {
		if (!(oldState instanceof IExtendedBlockState) || getRenderType(oldState) != EnumBlockRenderType.MODEL) {
			return oldState;
		}

		IExtendedBlockState state = (IExtendedBlockState) oldState;
		state = state.withProperty(BlockFluidBase.FLOW_DIRECTION, (float)getFlowDirection(worldIn, pos));
		IBlockState[][] upBlockState = new IBlockState[3][3];
		float[][] height = new float[3][3];
		upBlockState[1][1] = worldIn.getBlockState(pos.up());
		height[1][1] = getFluidHeightForRender(worldIn, pos, upBlockState[1][1]);
		if (height[1][1] == 1) {
			state = state.withProperty(BlockFluidBase.LEVEL_CORNERS[0], 1f)
					.withProperty(BlockFluidBase.LEVEL_CORNERS[1], 1f)
					.withProperty(BlockFluidBase.LEVEL_CORNERS[2], 1f)
					.withProperty(BlockFluidBase.LEVEL_CORNERS[3], 1f);
		} else {
			float[][] corner = new float[2][2];
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					if (i != 1 || j != 1) {
						BlockPos pos2 = pos.add(i - 1, 0, j - 1);
						upBlockState[i][j] = worldIn.getBlockState(pos2.up());
						height[i][j] = getFluidHeightForRender(worldIn, pos2, upBlockState[i][j]);
					}
				}
			}
			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < 2; j++) {
					corner[i][j] = getFluidHeightAverage(height[i][j], height[i][j + 1], height[i + 1][j], height[i + 1][j + 1]);
				}
			}
			//check for downflow above corners
			boolean n = isFluid(upBlockState[0][1]);
			boolean s = isFluid(upBlockState[2][1]);
			boolean w = isFluid(upBlockState[1][0]);
			boolean e = isFluid(upBlockState[1][2]);
			if (n || w || isFluid(upBlockState[0][0])) corner[0][0] = 1;
			if (n || e || isFluid(upBlockState[0][2])) corner[0][1] = 1;
			if (s || w || isFluid(upBlockState[2][0])) corner[1][0] = 1;
			if (s || e || isFluid(upBlockState[2][2])) corner[1][1] = 1;

			state = state.withProperty(BlockFluidBase.LEVEL_CORNERS[0], corner[0][0])
					.withProperty(BlockFluidBase.LEVEL_CORNERS[1], corner[0][1])
					.withProperty(BlockFluidBase.LEVEL_CORNERS[2], corner[1][1])
					.withProperty(BlockFluidBase.LEVEL_CORNERS[3], corner[1][0]);
		}

		return state;
	}

	private boolean isFluid(IBlockState state) {
		return state.getMaterial().isLiquid() || state.getBlock() instanceof IFluidBlock;
	}
}
