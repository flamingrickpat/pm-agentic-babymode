package com.agenticbabymode;

import com.agenticbabymode.command.BabymodeCommand;
import com.agenticbabymode.command.WorldCheckpointCommand;
import com.agenticbabymode.config.ConfigManager;
import com.agenticbabymode.server.BabymodeServer;
import com.agenticbabymode.server.WorldCheckpoint;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgenticBabymodeMod implements ModInitializer {
	public static final String MOD_ID = "agentic_babymode";
	public static final String MOD_VERSION = "0.4.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Agentic Babymode v{} initializing", MOD_VERSION);
		ConfigManager.init();
		BabymodeServer.init();
		WorldCheckpoint.init();
		BabymodeCommand.register();
		WorldCheckpointCommand.register();
		LOGGER.info("Agentic Babymode v{} ready", MOD_VERSION);
	}
}
