package uninenville.blocktodisplay;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uninenville.blocktodisplay.command.BlockToDisplayCommand;

import java.util.List;

public class BlockToDisplay implements ModInitializer {
	public static final String MOD_ID = "blocktodisplay";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final MinecraftClient CLIENT = MinecraftClient.getInstance();

	public static final AffineTransformation DEFAULT_TRANSFORMATION = new AffineTransformation(
		new Vector3f(-0.5F, -0.5F, -0.5F),
		new Quaternionf(),
		new Vector3f(1.0F, 1.0F, 1.0F),
		new Quaternionf()
	);

	public static List<TagKey<Block>> unsupportedTags = List.of(
		BlockTags.ALL_SIGNS,
		BlockTags.BANNERS,
		BlockTags.BEDS,
		BlockTags.SHULKER_BOXES
	);
	public static List<Block> unsupportedBlocks = List.of(
		Blocks.BARRIER,
		Blocks.LIGHT,
		Blocks.STRUCTURE_VOID,
		Blocks.DECORATED_POT,
		Blocks.END_GATEWAY,
		Blocks.END_PORTAL,
		Blocks.SKELETON_SKULL,
		Blocks.SKELETON_WALL_SKULL,
		Blocks.WITHER_SKELETON_SKULL,
		Blocks.WITHER_SKELETON_WALL_SKULL,
		Blocks.PLAYER_HEAD,
		Blocks.PLAYER_WALL_HEAD,
		Blocks.ZOMBIE_HEAD,
		Blocks.ZOMBIE_WALL_HEAD,
		Blocks.CREEPER_HEAD,
		Blocks.CREEPER_WALL_HEAD,
		Blocks.PIGLIN_HEAD,
		Blocks.PIGLIN_WALL_HEAD,
		Blocks.DRAGON_HEAD,
		Blocks.DRAGON_WALL_HEAD,
		Blocks.CHEST,
		Blocks.TRAPPED_CHEST,
		Blocks.ENDER_CHEST
	);

	public static boolean changeBlockToDisplayOnBreak = false;

	@Override
	public void onInitialize() {
		ClientCommandRegistrationCallback.EVENT.register(BlockToDisplayCommand::register);
		ClientPlayerBlockBreakEvents.AFTER.register(BlockToDisplay::changeBlockToDisplayOnBreak);
	}

	private static void changeBlockToDisplayOnBreak(ClientWorld world, ClientPlayerEntity player, BlockPos pos, BlockState state) {
		if (player.isCreative() && changeBlockToDisplayOnBreak) {
			tryCreateBlockDisplay(player, pos, state);
		}
	}

	public static boolean tryCreateBlockDisplay(ClientPlayerEntity player, BlockPos pos, BlockState state) {
		if (canCreateDisplay(state)) {
			NbtCompound nbt = new NbtCompound();
			nbt.put("block_state", NbtHelper.fromBlockState(state));
			AffineTransformation.ANY_CODEC
				.encodeStart(NbtOps.INSTANCE, DEFAULT_TRANSFORMATION)
				.ifSuccess(transformations -> nbt.put("transformation", transformations));

			Vec3d center = pos.toCenterPos();
			String command = String.format("summon block_display %s %s %s %s", center.getX(), center.getY(), center.getZ(), nbt);
			if (CLIENT.getNetworkHandler() != null) {
				return CLIENT.getNetworkHandler().sendCommand(command);
			}
		} else {
			player.sendMessage(Text.translatable("blocktodisplay.error.unsupported_block", state.getBlock().getName()));
		}

		return false;
	}

	public static boolean canCreateDisplay(BlockState state) {
		for (TagKey<Block> tag : unsupportedTags) {
			if (state.isIn(tag)) {
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
