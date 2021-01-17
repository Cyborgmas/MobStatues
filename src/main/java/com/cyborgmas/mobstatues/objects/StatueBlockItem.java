package com.cyborgmas.mobstatues.objects;

import com.cyborgmas.mobstatues.MobStatues;
import com.cyborgmas.mobstatues.registration.Registration;
import com.cyborgmas.mobstatues.util.StatueCreationHelper;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class StatueBlockItem extends BlockItem {
    public StatueBlockItem(Properties properties) {
        super(Registration.STATUE_BLOCK.get(), properties);
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        if (!stack.hasTag() || !stack.getOrCreateTag().contains("id"))
            return;

        EntityType<?> entity = EntityType.readEntityType(stack.getOrCreateTag()).orElse(null);

        TranslationTextComponent entityName = entity != null ?
                new TranslationTextComponent(entity.getTranslationKey()) : MobStatues.translate("entity", "unknown");
        tooltip.add(MobStatues.translate("tooltip", "statue", entityName));
    }

    @Override
    protected boolean placeBlock(BlockItemUseContext context, BlockState state) {
        if (!context.getItem().hasTag())
            return false;

        CompoundNBT nbt = context.getItem().getOrCreateTag();
        Direction lookingDir = context.getPlacementHorizontalFacing();
        BlockPos start = context.getPos();
        World world = context.getWorld();

        Pair<List<BlockPos>, Boolean> toPlace = getPlacements(start, world, StatueCreationHelper.getEntitySize(nbt).withWorld(world), lookingDir);
        if (toPlace == null)
            return false;

        if (!toPlace.getFirst().remove(start)) // shouldn't happen?
            return false;

        boolean ret = world.setBlockState(start, state, Constants.BlockFlags.DEFAULT_AND_RERENDER);

        TileEntity te = world.getTileEntity(context.getPos());
        if (!(te instanceof StatueTileEntity))
            return false;

        ((StatueTileEntity) te).setup(nbt, toPlace, start, lookingDir.getOpposite());

        for (BlockPos p : toPlace.getFirst()) {
            if (!world.setBlockState(p, state.with(StatueBlock.START, false), Constants.BlockFlags.DEFAULT))
                ret = false;
            if (world.getTileEntity(p) instanceof DelegatingTileEntity)
                ((DelegatingTileEntity) world.getTileEntity(p)).setDelegate(start);
            else
                ret = false;
        }
        return ret;
    }

    @Nullable
    public static Pair<List<BlockPos>, Boolean> getPlacements(BlockPos placed, World world, EntitySize size, Direction lookingDir) {
        // Can't place relative to up/down.
        if (lookingDir.getHorizontalIndex() == -1) {
            MobStatues.LOGGER.error("Oh no! Something went terribly wrong.");
            return null;
        }

        List<BlockPos> ret = Lists.newArrayList(placed.toImmutable());
        if (size.height <= 1 && size.width <= 1)
            return Pair.of(ret, null);
        int h = MathHelper.ceil(size.height - 1);
        int w = MathHelper.ceil(size.width - 1);

        ret.addAll(extendInDir(ret, Direction.UP, h));
        boolean canPlace = verify(ret, world);
        if (!canPlace) // for now we aren't checking down only up.
            return null;
        if (w == 0)
            return Pair.of(ret, null); //null for no width.

        ret.addAll(extendInDir(ret, lookingDir, w));

        if (!verify(ret, world)) // We extended in the direction of the look vector, don't go opposite of that.
            return null;

        List<BlockPos> copy = Lists.newArrayList(ret);
        ret.addAll(extendInDir(ret, lookingDir.rotateY(), w)); // right of the look vector

        if (!verify(ret, world)) {
            ret = Lists.newArrayList(copy);
            ret.addAll(extendInDir(ret, lookingDir.rotateYCCW(), w)); // left of the look vector
            return verify(ret, world) ? Pair.of(ret, true) : null;
        }
        else
            return Pair.of(ret, false);
    }

    private static List<BlockPos> extendInDir(List<BlockPos> placed, Direction direction, int reps) {
        List<BlockPos> ret = new ArrayList<>();
        for (int i = 1; i <= reps; i++) {
            for (BlockPos pos : placed) {
                ret.add(pos.offset(direction, i));
            }
        }
        return ret;
    }

    private static boolean verify(List<BlockPos> toVerify, World world) {
        return toVerify.stream().allMatch(world::isAirBlock);
    }
}
