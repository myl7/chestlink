package io.github.myl7.chestlink;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class ChestLinkMod implements ModInitializer {
	public static final String MOD_ID = "chestlink";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register(
				(dispatcher, buildContext, selection) -> ChestLinkCommand.register(dispatcher));

		ServerLifecycleEvents.SERVER_STARTED.register(LinkManager::load);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> LinkManager.unload());
		ServerTickEvents.END_SERVER_TICK.register(LinkManager::tick);

		LOGGER.info("ChestLink initialized");
	}
}
