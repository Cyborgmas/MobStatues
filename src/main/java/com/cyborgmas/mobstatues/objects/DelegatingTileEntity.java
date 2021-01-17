package com.cyborgmas.mobstatues.objects;

import com.cyborgmas.mobstatues.registration.Registration;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;

public class DelegatingTileEntity extends TileEntity {
    private static final String DELEGATED_POS_KEY = "delegated_tile_pos";
    // No need to render this TE. Empty VoxelShape doesn't have a bounding box.
    private static final AxisAlignedBB RENDER_AABB_CACHE = VoxelShapes.fullCube().getBoundingBox();
    private BlockPos delegatedPos = null;

    public DelegatingTileEntity() {
        super(Registration.DELEGATING_TILE.get());
    }

    public void setDelegate(BlockPos delegatedPos) {
        this.delegatedPos = delegatedPos;
    }

    @Nullable
    public <T extends TileEntity> T getDelegate(TileEntityType<T> type, IBlockReader reader) {
        if (this.delegatedPos != null) {
            TileEntity te = reader.getTileEntity(this.delegatedPos);
            return te != null && te.getType() == type ? (T) te : null;
        }
        return null;
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return RENDER_AABB_CACHE;
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        if (delegatedPos != null)
            compound.putLong(DELEGATED_POS_KEY, delegatedPos.toLong());
        return super.write(compound);
    }

    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        if (nbt.contains(DELEGATED_POS_KEY, Constants.NBT.TAG_LONG))
            this.delegatedPos = BlockPos.fromLong(nbt.getLong(DELEGATED_POS_KEY));
    }

    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT nbt) {
        if (nbt.contains(DELEGATED_POS_KEY, Constants.NBT.TAG_LONG))
            this.delegatedPos = BlockPos.fromLong(nbt.getLong(DELEGATED_POS_KEY));
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT nbt = super.getUpdateTag();
        if (delegatedPos != null)
            nbt.putLong(DELEGATED_POS_KEY, delegatedPos.toLong());
        return nbt;
    }
}
