package com.agenticbabymode;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgenticBabymodeMod implements ModInitializer {
	public static final String MOD_ID = "agentic_babymode";
	public static final String MOD_VERSION = "0.1.0";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Agentic Babymode v{} initialized", MOD_VERSION);
	}
}