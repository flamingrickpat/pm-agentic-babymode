package com.agenticbabymode.command;

import com.agenticbabymode.server.WorldCheckpoint;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.Locale;

import static net.minecraft.server.command.CommandManager.literal;

public final class WorldCheckpointCommand {
	private WorldCheckpointCommand() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("commit")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(WorldCheckpointCommand::commit));
			dispatcher.register(literal("revert")
					.requires(source -> source.hasPermissionLevel(2))
					.executes(WorldCheckpointCommand::revert));
		});
	}

	private static int commit(CommandContext<ServerCommandSource> context) {
		long startedAt = System.nanoTime();
		WorldCheckpoint.CommitResult result = WorldCheckpoint.INSTANCE.commit();
		context.getSource().sendFeedback(Text.literal(String.format(Locale.ROOT,
				"Committed world baseline from %d loaded chunk(s) and %d block entity(s) in %.2f ms",
				result.loadedChunks(), result.blockEntities(), elapsedMillis(startedAt))), true);
		return 1;
	}

	private static int revert(CommandContext<ServerCommandSource> context) {
		if (!WorldCheckpoint.INSTANCE.exists()) {
			context.getSource().sendError(Text.literal("No world checkpoint exists. Run /commit first."));
			return 0;
		}

		WorldCheckpoint.RevertResult result = WorldCheckpoint.INSTANCE.revert(context.getSource().getServer());
		context.getSource().sendFeedback(Text.literal(String.format(Locale.ROOT,
				"Reverted %d block(s) and %d block entity change(s) in %d chunk(s) in %.2f ms",
				result.blocks(), result.blockEntities(), result.chunks(), result.elapsedNanos() / 1_000_000.0)), true);
		return 1;
	}

	private static double elapsedMillis(long startedAt) {
		return (System.nanoTime() - startedAt) / 1_000_000.0;
	}
}
