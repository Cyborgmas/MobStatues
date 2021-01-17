package com.cyborgmas.mobstatues.objects;

import com.cyborgmas.mobstatues.MobStatues;
import com.cyborgmas.mobstatues.registration.Registration;
import com.cyborgmas.mobstatues.util.LazyOptionalFunction;
import com.cyborgmas.mobstatues.util.StatueCreationHelper;
import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Util;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraft.world.IWorld;
import net.minecraftforge.common.util.Constants;

import java.util.*;
import java.util.stream.Collectors;

public class StatueTileEntity extends TileEntity {
    private static final String STATUE_PARTS_KEY = "statue_parts_positions";
    private static final String STATUE_DIRECTION_KEY = "statue_direction";
    private static final String STATUE_TO_CENTER_KEY = "statue_to_center";
    private static final Map<StatueTileEntity, Map<BlockPos, Pair<VoxelShape, VoxelShape>>> SHAPES = new WeakHashMap<>();

    //Fall backs
    private static final Pair<VoxelShape, VoxelShape> DEFAULT = Pair.of(VoxelShapes.fullCube(), VoxelShapes.fullCube());
    private static final CompoundNBT NBT_PIG = Util.make(() -> {
       CompoundNBT ret = new CompoundNBT();
       ret.putString("id", "pig");
       return ret;
    });

    // Serialized fields
    private CompoundNBT entityData = new CompoundNBT();
    private List<BlockPos> wholeStatue = null;
    private Vector3d toCenter = new Vector3d(0.5, 0, 0.5);
    private Direction direction = Direction.SOUTH;

    // Computed fields.
    private Entity statue = null;
    // Weird? Not sure how useful.
    private LazyOptionalFunction<CompoundNBT, EntityType<?>> type = new LazyOptionalFunction<>(EntityType::readEntityType);

    public StatueTileEntity() {
        super(Registration.STATUE_TILE.get());
    }

    public void setup(CompoundNBT entityData, Pair<List<BlockPos>, Boolean> delegates, BlockPos placed, Direction direction) {
        this.entityData = entityData;
        this.type.apply(this.entityData);
        this.wholeStatue = ImmutableList.<BlockPos>builder().addAll(delegates.getFirst()).add(placed).build();
        this.direction = direction;
        this.findStatueCenter(delegates.getFirst(), delegates.getSecond());
    }

    public void setEntity(Entity statue) {
        this.statue = statue;
    }

    public EntityType<?> getEntityType() {
        return this.type.get();
    }

    /**
     * @param toLeft can be null, so leave it boxed.
     */
    private void findStatueCenter(List<BlockPos> placements, Boolean toLeft) {
        BlockPos thisPos = this.getPos();
        Vector3i increment1 = this.direction.getOpposite().getDirectionVec();
        Vector3i increment2 = toLeft == null ? Vector3i.NULL_VECTOR : (toLeft ? this.direction.rotateY() : this.direction.rotateYCCW()).getDirectionVec();
        Vector3i increment = new BlockPos(increment1).add(increment2); // Bcs Vector3i doesn't have #add

        //Starting position is 0, 0 in the NW corner. adding +0.5 centers it
        double x = 0.5;
        double z = 0.5;
        for (BlockPos pos : placements) { // list doesn't contain the pos of this TE.
            if (pos.getY() == thisPos.getY()) { // don't consider y diffs
                // Add to one coord if the other is the same to not count corners.
                if (pos.getZ() == thisPos.getZ())
                    x += (increment.getX() / 2D);
                else if (pos.getX() == thisPos.getX())
                    z += (increment.getZ() / 2D);
            }
        }

        this.toCenter = new Vector3d(x, 0, z);
    }

    public void checkDestroyStatue(IWorld world, BlockPos changed) {
        if (this.wholeStatue != null &&
                this.wholeStatue.contains(changed) &&
                world.getBlockState(changed) == Blocks.AIR.getDefaultState()
        ) {
            for (BlockPos pos : this.wholeStatue) {
                if (world.getBlockState(pos).getBlock() == Registration.STATUE_BLOCK.get())
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), Constants.BlockFlags.DEFAULT);
            }
        }
    }

    public void destroyStatue(IWorld world, BlockPos ignore) {
        if (this.wholeStatue != null)
            for (BlockPos pos : this.wholeStatue) {
                if (!pos.equals(ignore) && world.getBlockState(pos).getBlock() == Registration.STATUE_BLOCK.get())
                    world.setBlockState(pos, Blocks.AIR.getDefaultState(), Constants.BlockFlags.DEFAULT);
        }
    }

    /**
     * First is "rendering" shape
     * Second is "collision" shape.
     */
    public Pair<VoxelShape, VoxelShape> getBothShapes(BlockPos looking) {
        if(this.type.get() != null && this.wholeStatue != null && this.wholeStatue.contains(looking)) {
            return SHAPES.computeIfAbsent(this, tile ->
                            computeShapesFor(tile, tile.pos, tile.wholeStatue)
                    ).computeIfAbsent(looking, pos -> {
                        MobStatues.LOGGER.error("Something went wrong when calculating the statue's collision boxes!");
                        MobStatues.LOGGER.error("Position: {}\nWholeStatue: {}\nRendering: {}", this.pos, this.wholeStatue, pos);
                        return DEFAULT;
                    });
        }
        return DEFAULT;
    }

    public void renderEntity(MatrixStack stack, float partialTicks, IRenderTypeBuffer buffer, int light) {
        if (this.entityData.isEmpty())
            return;

        if (this.statue == null) {
            this.statue = StatueCreationHelper.getEntity(this.entityData, true).get();
            if (this.statue == null)
                return;
            this.statue.setRotationYawHead(0);
        }

        stack.push();

        stack.translate(this.toCenter.getX(), 0, this.toCenter.getZ());

        Quaternion rotation = Vector3f.YP.rotationDegrees(90 * ((4 - this.direction.getHorizontalIndex()) % 4));
        stack.rotate(rotation);

        Minecraft.getInstance().getRenderManager().getRenderer(statue).render(statue, 0, partialTicks, stack, buffer, light);

        stack.pop();
    }

    private static Map<BlockPos, Pair<VoxelShape, VoxelShape>> computeShapesFor(StatueTileEntity tile, BlockPos start, List<BlockPos> parts) {
        Map<BlockPos, Pair<VoxelShape, VoxelShape>> tileShapes = new HashMap<>();

        VoxelShape og = StatueCreationHelper.getShape(tile.entityData).withWorld(tile.world);

        og = og.withOffset(tile.toCenter.x, tile.toCenter.y, tile.toCenter.z);
        // From my understanding, returns the intersection of the 2 shapes.
        VoxelShape ogCollision = VoxelShapes.combineAndSimplify(og, VoxelShapes.fullCube(), IBooleanFunction.AND);

        tileShapes.put(start, Pair.of(og, ogCollision));

        for (BlockPos pos : parts) {
            if (!pos.equals(start)) {
                int x = start.getX() - pos.getX();
                int y = start.getY() - pos.getY();
                int z = start.getZ() - pos.getZ();
                VoxelShape rendering = og.withOffset(x, y, z);
                // Not sure if I could improve this creation. Gives each block the appropriate collision box for where
                // they are, instead of giving them the full rendering shape but offset. This is so that in a X piece
                // statue there aren't X collision boxes for the single statue.
                VoxelShape offsetCube = VoxelShapes.fullCube().withOffset(-x, -y, -z);
                VoxelShape collision = VoxelShapes.combineAndSimplify(og, offsetCube, IBooleanFunction.AND).withOffset(x, y, z);

                tileShapes.put(pos, Pair.of(rendering, collision));
            }
        }
        return tileShapes;
    }

    private CompoundNBT bothSidedWrite(CompoundNBT nbt) {
        if (this.wholeStatue != null) {
            nbt.put(STATUE_PARTS_KEY, new LongArrayNBT(
                    this.wholeStatue.stream().map(BlockPos::toLong).collect(Collectors.toList())
            ));
        }

        nbt.put(MobStatues.MOB_STATUE_KEY, this.entityData);

        ListNBT list = new ListNBT();
        list.add(DoubleNBT.valueOf(this.toCenter.x));
        list.add(DoubleNBT.valueOf(this.toCenter.y));
        list.add(DoubleNBT.valueOf(this.toCenter.z));

        nbt.put(STATUE_TO_CENTER_KEY, list);

        nbt.put(STATUE_DIRECTION_KEY, IntNBT.valueOf(this.direction.getHorizontalIndex()));

        return nbt;
    }

    private void bothSidedRead(CompoundNBT nbt) {
        this.entityData = nbt.contains(MobStatues.MOB_STATUE_KEY, Constants.NBT.TAG_COMPOUND) ?
                nbt.getCompound(MobStatues.MOB_STATUE_KEY) : NBT_PIG;

        this.type.apply(this.entityData);

        if (nbt.contains(STATUE_TO_CENTER_KEY, Constants.NBT.TAG_LIST)) {
            ListNBT list = nbt.getList(STATUE_TO_CENTER_KEY, Constants.NBT.TAG_DOUBLE);
            if (list.size() == 3)
                this.toCenter = new Vector3d(list.getDouble(0), list.getDouble(1), list.getDouble(2));
        }

        if (nbt.contains(STATUE_DIRECTION_KEY, Constants.NBT.TAG_INT))
            this.direction = Direction.byHorizontalIndex(nbt.getInt(STATUE_DIRECTION_KEY));

        if (nbt.contains(STATUE_PARTS_KEY, Constants.NBT.TAG_LONG_ARRAY))
            this.wholeStatue = Arrays.stream(nbt.getLongArray(STATUE_PARTS_KEY))
                    .mapToObj(BlockPos::fromLong)
                    .collect(Collectors.toList());
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        return getBothShapes(this.pos).getFirst().getBoundingBox().offset(this.pos);
    }

    /**
     * Client write
     */
    @Override
    public CompoundNBT getUpdateTag() {
        return bothSidedWrite(super.getUpdateTag());
    }

    /**
     * Client read
     */
    @Override
    public void handleUpdateTag(BlockState state, CompoundNBT tag) {
        this.bothSidedRead(tag);
    }

    /**
     * Server write
     */
    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        return this.bothSidedWrite(super.write(nbt));
    }

    /**
     * Server read
     */
    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        this.bothSidedRead(nbt);
    }
}
