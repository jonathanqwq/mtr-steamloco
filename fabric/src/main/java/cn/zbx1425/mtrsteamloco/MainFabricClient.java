package cn.zbx1425.mtrsteamloco;

import cn.zbx1425.mtrsteamloco.gui.ConfigScreen;
import cn.zbx1425.mtrsteamloco.render.SteamSmokeParticle;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import mtr.client.ICustomResources;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v1.ClientCommandManager;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.event.client.ClientSpriteRegistryCallback;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class MainFabricClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientSpriteRegistryCallback.event(InventoryMenu.BLOCK_ATLAS).register(((atlasTexture, registry) -> {
			registry.register(new ResourceLocation(Main.MOD_ID, "particle/steam_smoke"));
		}));

		ParticleFactoryRegistry.getInstance().register(Main.PARTICLE_STEAM_SMOKE, SteamSmokeParticle.Provider::new);

		ClientCommandManager.DISPATCHER.register(
			ClientCommandManager.literal("mtrsteamloco")
				.then(ClientCommandManager.literal("config")
					.executes(context -> {
					Minecraft.getInstance().tell(() -> {
						Minecraft.getInstance().setScreen(new ConfigScreen(Minecraft.getInstance().screen));
					});
					return 1;
				}))
		);

		MainClient.init();
	}

}
