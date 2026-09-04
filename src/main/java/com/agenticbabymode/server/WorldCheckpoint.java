package com.agenticbabymode.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * An in-memory checkpoint for fast test-world rollback.
 *
 * Logic errors throw. A missing checkpoint is reported as a command error. A server restart deliberately
 * discards the checkpoint; this is an undo journal, not a durable world backup.
 */
public final class WorldCheckpoint {
	public static final WorldCheckpoint INSTANCE = new WorldCheckpoint();

	private static final int RESTORE_FLAGS = Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS;

	private final Map<BlockKey, OriginalBlock> changedBlocks = new LinkedHashMap<>();
	private final Map<BlockKey, NbtCompound> baselineBlockEntities = new LinkedHashMap<>();
	private final Set<BlockKey> changedBlockEntities = new LinkedHashSet<>();
	private final Set<WorldChunk> loadedChunks = Collections.newSetFromMap(new IdentityHashMap<>());

	private boolean committed;
	private boolean reverting;

	private WorldCheckpoint() {
	}

	public static void init() {
		ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> INSTANCE.chunkLoaded(chunk));
		ServerChunkEvents.CHUNK_UNLOAD.register((world, chunk) -> INSTANCE.loadedChunks.remove(chunk));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> INSTANCE.clear());
	}

	public CommitResult commit() {
		changedBlocks.clear();
		baselineBlockEntities.clear();
		changedBlockEntities.clear();
		committed = true;

		for (WorldChunk chunk : loadedChunks) {
			snapshotBlockEntities(chunk);
		}

		return new CommitResult(loadedChunks.size(), baselineBlockEntities.size());
	}

	public boolean exists() {
		return committed;
	}

	public RevertResult revert(MinecraftServer server) {
		if (!committed) {
			throw new IllegalStateException("No world checkpoint exists. Run /commit first.");
		}

		long startedAt = System.nanoTime();
		Set<ChunkKey> dirtyChunks = dirtyChunks();
		reverting = true;
		try {
			clearPendingUpdates(server, dirtyChunks);
			restoreBlocks(server);
			restoreBlockEntities(server);
			wakeSleepingPlayers(server);
			clearPendingUpdates(server, dirtyChunks);
		} finally {
			reverting = false;
		}

		int blockCount = changedBlocks.size();
		int blockEntityCount = changedBlockEntities.size();
		changedBlocks.clear();
		changedBlockEntities.clear();
		long elapsedNanos = System.nanoTime() - startedAt;
		return new RevertResult(blockCount, blockEntityCount, dirtyChunks.size(), elapsedNanos);
	}

	public void recordBlockChange(ServerWorld world, BlockPos pos, BlockState oldState, BlockState newState,
			BlockEntity oldBlockEntity) {
		if (!committed || reverting || oldState == newState) {
			return;
		}

		BlockKey key = new BlockKey(world.getRegistryKey(), pos.toImmutable());
		changedBlocks.computeIfAbsent(key, ignored -> new OriginalBlock(
				oldState,
				oldBlockEntity == null ? null : oldBlockEntity.createNbtWithIdentifyingData()
		));
	}

	public void recordBlockEntityChange(ServerWorld world, BlockPos pos) {
		if (!committed || reverting) {
			return;
		}
		changedBlockEntities.add(new BlockKey(world.getRegistryKey(), pos.toImmutable()));
	}

	private void chunkLoaded(WorldChunk chunk) {
		loadedChunks.add(chunk);
		if (committed && !reverting) {
			snapshotBlockEntities(chunk);
		}
	}

	private void snapshotBlockEntities(WorldChunk chunk) {
		if (!(chunk.getWorld() instanceof ServerWorld world)) {
			return;
		}
		for (Map.Entry<BlockPos, BlockEntity> entry : chunk.getBlockEntities().entrySet()) {
			BlockKey key = new BlockKey(world.getRegistryKey(), entry.getKey().toImmutable());
			baselineBlockEntities.putIfAbsent(key, entry.getValue().createNbtWithIdentifyingData());
		}
	}

	private Set<ChunkKey> dirtyChunks() {
		Set<ChunkKey> chunks = new LinkedHashSet<>();
		for (BlockKey key : changedBlocks.keySet()) {
			chunks.add(new ChunkKey(key.world(), new ChunkPos(key.pos()).toLong()));
		}
		for (BlockKey key : changedBlockEntities) {
			chunks.add(new ChunkKey(key.world(), new ChunkPos(key.pos()).toLong()));
		}
		return chunks;
	}

	private void wakeSleepingPlayers(MinecraftServer server) {
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (player.isSleeping()) {
				player.wakeUp(true, false);
			}
		}
	}

	private void clearPendingUpdates(MinecraftServer server, Set<ChunkKey> dirtyChunks) {
		for (ChunkKey key : dirtyChunks) {
			ServerWorld world = Objects.requireNonNull(server.getWorld(key.world()),
					() -> "Missing world " + key.world().getValue());
			ChunkPos chunk = new ChunkPos(key.pos());
			BlockBox box = new BlockBox(
					chunk.getStartX() - 1,
					world.getBottomY(),
					chunk.getStartZ() - 1,
					chunk.getEndX() + 1,
					world.getTopY() - 1,
					chunk.getEndZ() + 1
			);
			world.getBlockTickScheduler().clearNextTicks(box);
			world.getFluidTickScheduler().clearNextTicks(box);
			world.clearUpdatesInArea(box);
		}
	}

	private void restoreBlocks(MinecraftServer server) {
		for (Map.Entry<BlockKey, OriginalBlock> entry : changedBlocks.entrySet()) {
			BlockKey key = entry.getKey();
			ServerWorld world = Objects.requireNonNull(server.getWorld(key.world()),
					() -> "Missing world " + key.world().getValue());
			world.setBlockState(key.pos(), entry.getValue().state(), RESTORE_FLAGS);
		}
	}

	private void restoreBlockEntities(MinecraftServer server) {
		Map<BlockKey, NbtCompound> entitiesToRestore = new LinkedHashMap<>();
		for (Map.Entry<BlockKey, OriginalBlock> entry : changedBlocks.entrySet()) {
			if (entry.getValue().blockEntityNbt() != null) {
				entitiesToRestore.put(entry.getKey(), entry.getValue().blockEntityNbt());
			}
		}
		for (BlockKey key : changedBlockEntities) {
			NbtCompound baseline = baselineBlockEntities.get(key);
			if (baseline != null) {
				entitiesToRestore.put(key, baseline);
			}
		}

		for (Map.Entry<BlockKey, NbtCompound> entry : entitiesToRestore.entrySet()) {
			BlockKey key = entry.getKey();
			ServerWorld world = Objects.requireNonNull(server.getWorld(key.world()),
					() -> "Missing world " + key.world().getValue());
			BlockState state = world.getBlockState(key.pos());
			BlockEntity blockEntity = Objects.requireNonNull(
					BlockEntity.createFromNbt(key.pos(), state, entry.getValue().copy()),
					() -> "Could not restore block entity at " + key.pos()
			);
			world.removeBlockEntity(key.pos());
			world.addBlockEntity(blockEntity);
			blockEntity.markDirty();
			world.updateListeners(key.pos(), state, state, Block.NOTIFY_LISTENERS);
		}
	}

	private void clear() {
		changedBlocks.clear();
		baselineBlockEntities.clear();
		changedBlockEntities.clear();
		loadedChunks.clear();
		committed = false;
		reverting = false;
	}

	private record BlockKey(RegistryKey<World> world, BlockPos pos) {
	}

	private record ChunkKey(RegistryKey<World> world, long pos) {
	}

	private record OriginalBlock(BlockState state, NbtCompound blockEntityNbt) {
	}

	public record CommitResult(int loadedChunks, int blockEntities) {
	}

	public record RevertResult(int blocks, int blockEntities, int chunks, long elapsedNanos) {
	}
}
