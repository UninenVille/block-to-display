package xyz.uninenville.blocktodisplay.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import xyz.uninenville.blocktodisplay.BlockToDisplay;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class BlockToDisplayCommand {
    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(
            literal("blocktodisplay")
                .then(literal("toggle")
                    .executes(BlockToDisplayCommand::toggle)
                )

                .then(literal("replacenear")
                    .then(argument("block", BlockStateArgument.block(buildContext))
                        .then(argument("radius", IntegerArgumentType.integer(1))
                            .executes(BlockToDisplayCommand::replaceNearby)
                        )
                    )
                )
        );
    }

    private static int toggle(CommandContext<FabricClientCommandSource> context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;

        boolean enabled = BlockToDisplay.changeBlockToDisplayOnBreak;
        BlockToDisplay.changeBlockToDisplayOnBreak = !enabled;
        player.sendSystemMessage(Component.translatable("blocktodisplay.toggle." + (!enabled ? "on" : "off")));

        return 1;
    }

    private static int replaceNearby(CommandContext<FabricClientCommandSource> context) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return 0;

        Level level = player.level();
        Vec3 center = player.getOnPos().getCenter();
        int radius = IntegerArgumentType.getInteger(context, "radius");
        Block block = context.getArgument("block", BlockInput.class).getState().getBlock();

        AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
        if (box.minY < level.getMinY()) {
            box = box.setMinY(level.getMinY());
        }
        if (box.maxY > level.getMinY() + level.getHeight()) {
            box = box.setMaxY(level.getMinY() + level.getHeight());
        }

        BlockPos.betweenClosedStream(box).forEach(pos -> {
            if (!level.isLoaded(pos))
                return;

            BlockState state = level.getBlockState(pos);

            if (state.is(block)) {
                if (BlockToDisplay.tryCreateBlockDisplay(player, pos, state) && Minecraft.getInstance().getConnection() != null) {
                    Minecraft.getInstance().getConnection().sendCommand(String.format("setblock %d %d %d air", pos.getX(), pos.getY(), pos.getZ()));
                }
            }
        });

        return 1;
    }

}
