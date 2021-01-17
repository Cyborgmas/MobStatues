package com.cyborgmas.mobstatues.objects;

import com.cyborgmas.mobstatues.registration.Registration;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;

@SuppressWarnings({"deprecation", "NullableProblems"})
public class StatueBlock extends Block {
    public static BooleanProperty START = BooleanProperty.create("start");

    public StatueBlock(Properties properties) {
        super(properties);
        this.setDefaultState(this.getStateContainer().getBaseState().with(START, true));
    }

    @Nullable
    public StatueTileEntity getStatueTile(IBlockReader reader, BlockPos pos) {
        TileEntity te = reader.getTileEntity(pos);
        StatueTileEntity statue = null;
        if (te instanceof StatueTileEntity)
            statue = (StatueTileEntity) te;
        else if (te instanceof DelegatingTileEntity)
            statue = ((DelegatingTileEntity) te).getDelegate(Registration.STATUE_TILE.get(), reader);

        return statue;
    }

    public VoxelShape getShape(IBlockReader reader, BlockPos pos, boolean rendering) {
        StatueTileEntity statue = getStatueTile(reader, pos);

        if (statue != null) {
            Pair<VoxelShape, VoxelShape> pair = statue.getBothShapes(pos);
            return rendering ? pair.getFirst() : pair.getSecond();
        }
        return VoxelShapes.fullCube();
    }

    /**
     * Destroys the whole statue if the block destroyed is the starting block.
     */
    @Override
    public void onBlockHarvested(World worldIn, BlockPos pos, BlockState state, PlayerEntity player) {
        if (state.get(START)) {
            TileEntity te = worldIn.getTileEntity(pos);
            if (te instanceof StatueTileEntity)
                ((StatueTileEntity) te).destroyStatue(worldIn, pos);
        }

        super.onBlockHarvested(worldIn, pos, state, player);
    }

    /**
     * Destroys the whole statue if the block destroyed is not the starting block.
     */
    @Override
    public BlockState updatePostPlacement(BlockState thisState, Direction facing, BlockState changedState, IWorld world, BlockPos currentPos, BlockPos changedPos) {
        StatueTileEntity statue = getStatueTile(world, currentPos);

        if (statue != null)
            statue.checkDestroyStatue(world, changedPos);

        return super.updatePostPlacement(thisState, facing, changedState, world, currentPos, changedPos);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(START);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        return this.getShape(reader, pos, true);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, IBlockReader reader, BlockPos pos, ISelectionContext context) {
        return this.getShape(reader, pos, false);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return state.get(START) ? new StatueTileEntity() : new DelegatingTileEntity();
    }
}
