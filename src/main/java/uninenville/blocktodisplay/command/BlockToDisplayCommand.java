package uninenville.blocktodisplay.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.command.argument.BlockStateArgumentType;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import uninenville.blocktodisplay.BlockToDisplay;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
import static uninenville.blocktodisplay.BlockToDisplay.CLIENT;

public class BlockToDisplayCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
            literal("blocktodisplay")
                .then(literal("toggle")
                    .executes(BlockToDisplayCommand::toggle)
                )

                .then(literal("replacenear")
                    .then(argument("block", BlockStateArgumentType.blockState(registryAccess))
                        .then(argument("radius", IntegerArgumentType.integer(1))
                            .executes(BlockToDisplayCommand::executes)
                        )
                    )
                )
        );
    }

    private static int toggle(CommandContext<FabricClientCommandSource> context) {
        if (CLIENT.player == null) return 0;

        boolean enabled = BlockToDisplay.changeBlockToDisplayOnBreak;
        BlockToDisplay.changeBlockToDisplayOnBreak = !enabled;
        CLIENT.player.sendMessage(Text.translatable("blocktodisplay.toggle." + (!enabled ? "on" : "off")));

        return 1;
    }

    private static int executes(CommandContext<FabricClientCommandSource> context) {
        if (CLIENT.player == null) return 0;

        World world = CLIENT.player.getWorld();
        Vec3d center = CLIENT.player.getBlockPos().toCenterPos();
        int radius = IntegerArgumentType.getInteger(context, "radius");
        Block block = context.getArgument("block", BlockStateArgument.class).getBlockState().getBlock();

        Box box = new Box(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
        if (box.minY < world.getBottomY()) {
            box = box.withMinY(world.getBottomY());
        }
        if (box.maxY > world.getTopY()) {
            box = box.withMaxY(world.getTopY());
        }

        BlockPos.stream(box).forEach(pos -> {
            if (!world.isChunkLoaded(ChunkSectionPos.getSectionCoord(pos.getX()), ChunkSectionPos.getSectionCoord(pos.getZ()))) return;

            BlockState state = world.getBlockState(pos);

            if (state.isOf(block)) {
                if (BlockToDisplay.tryCreateBlockDisplay(CLIENT.player, pos, state) && CLIENT.getNetworkHandler() != null) {
                    CLIENT.getNetworkHandler().sendCommand(String.format("setblock %d %d %d air", pos.getX(), pos.getY(), pos.getZ()));
                }
            }
        });

        return 1;
    }

}
