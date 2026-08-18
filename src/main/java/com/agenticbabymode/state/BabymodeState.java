package com.agenticbabymode.state;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World save data (PersistentState) holding per-UUID player state:
 * fatigue, grain, protein, produce.
 */
public class BabymodeState extends PersistentState {
	public static final String ID = "agentic_babymode";

	private final Map<UUID, PlayerState> players = new HashMap<>();

	public PlayerState getOrCreate(UUID uuid) {
		return players.computeIfAbsent(uuid, id -> PlayerState.defaults());
	}

	public PlayerState get(UUID uuid) {
		return players.get(uuid);
	}

	public Map<UUID, PlayerState> getPlayers() {
		return players;
	}

	@Override
	public NbtCompound writeNbt(NbtCompound nbt) {
		NbtList list = new NbtList();
		for (Map.Entry<UUID, PlayerState> entry : players.entrySet()) {
			NbtCompound tag = new NbtCompound();
			tag.putUuid("uuid", entry.getKey());
			PlayerState ps = entry.getValue();
			tag.putDouble("sleepiness", ps.sleepiness);
			tag.putDouble("grain", ps.grain);
			tag.putDouble("protein", ps.protein);
			tag.putDouble("produce", ps.produce);
			list.add(tag);
		}
		nbt.put("players", list);
		return nbt;
	}

	public static BabymodeState fromNbt(NbtCompound nbt) {
		BabymodeState state = new BabymodeState();
		if (nbt.contains("players", NbtElement.LIST_TYPE)) {
			NbtList list = nbt.getList("players", NbtElement.COMPOUND_TYPE);
			for (int i = 0; i < list.size(); i++) {
				NbtCompound tag = list.getCompound(i);
				UUID uuid = tag.getUuid("uuid");
				PlayerState ps = PlayerState.defaults();
				ps.sleepiness = tag.contains("sleepiness") ? tag.getDouble("sleepiness") : tag.getDouble("fatigue");
				ps.grain = tag.getDouble("grain");
				ps.protein = tag.getDouble("protein");
				ps.produce = tag.getDouble("produce");
				state.players.put(uuid, ps);
			}
		}
		return state;
	}
}
