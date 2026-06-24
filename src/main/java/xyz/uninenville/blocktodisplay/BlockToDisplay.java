package xyz.uninenville.blocktodisplay;

import com.mojang.math.Transformation;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.uninenville.blocktodisplay.command.BlockToDisplayCommand;

import java.util.List;

public class BlockToDisplay implements ModInitializer {
    public static final String MOD_ID = "block-to-display";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Transformation DEFAULT_TRANSFORMATION = new Transformation(
        new Vector3f(-0.5F, -0.5F, -0.5F),
        new Quaternionf(),
        new Vector3f(1.0F, 1.0F, 1.0F),
        new Quaternionf()
    );

    public static List<TagKey<Block>> unsupportedTags = List.of();

    public static List<Block> unsupportedBlocks = List.of(
        Blocks.BARRIER,
        Blocks.LIGHT,
        Blocks.STRUCTURE_VOID,
        Blocks.WATER,
        Blocks.LAVA
    );

    public static boolean changeBlockToDisplayOnBreak = false;

    @Override
    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register(BlockToDisplayCommand::register);
        ClientPlayerBlockBreakEvents.AFTER.register(BlockToDisplay::changeBlockToDisplayOnBreak);
    }

    private static void changeBlockToDisplayOnBreak(Level lever, LocalPlayer player, BlockPos pos, BlockState state) {
        if (player.isCreative() && changeBlockToDisplayOnBreak) {
            tryCreateBlockDisplay(player, pos, state);
        }
    }

    public static boolean tryCreateBlockDisplay(LocalPlayer player, BlockPos pos, BlockState state) {
        if (canCreateDisplay(state)) {
            CompoundTag nbt = new CompoundTag();
            nbt.put("block_state", NbtUtils.writeBlockState(state));
            Transformation.EXTENDED_CODEC
                .encodeStart(NbtOps.INSTANCE, DEFAULT_TRANSFORMATION)
                .ifSuccess(transformations -> nbt.put("transformation", transformations));

            Vec3 center = Vec3.atCenterOf(pos);
            String command = String.format("summon block_display %s %s %s %s", center.x(), center.y(), center.z(), nbt);
            if (Minecraft.getInstance().getConnection() != null) {
                Minecraft.getInstance().getConnection().sendCommand(command);
                return true;
            }
        } else {
            player.sendSystemMessage(Component.translatable("blocktodisplay.error.unsupported_block", state.getBlock().getName()));
        }

        return false;
    }

    public static boolean canCreateDisplay(BlockState state) {
        for (TagKey<Block> tag : unsupportedTags) {
            if (state.is(tag)) {
                return false;
            }
            for (Block block : unsupportedBlocks) {
                if (state.getBlock().equals(block)) {
                    return false;
                }
            }
        }

        return true;
    }

}
