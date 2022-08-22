package cn.zbx1425.mtrsteamloco;

import cn.zbx1425.mtrsteamloco.block.BlockDepartureBell;
import cn.zbx1425.mtrsteamloco.block.BlockFeedbackBox;
import cn.zbx1425.mtrsteamloco.block.BlockStatisticTurnstile;
import cn.zbx1425.mtrsteamloco.network.PacketFeedback;
import mtr.ItemGroups;
import mtr.RegistryObject;
import mtr.mappings.BlockEntityMapper;
import mtr.mappings.RegistryUtilities;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.BiConsumer;

public class Main {

	public static final String MOD_ID = "mtrsteamloco";

	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	public static final RegistryObject<Block> BLOCK_DEPARTURE_BELL = new RegistryObject<>(BlockDepartureBell::new);

	public static final RegistryObject<Block> BLOCK_STATISTIC_TURNSTILE = new RegistryObject<>(BlockStatisticTurnstile::new);
	public static final RegistryObject<BlockEntityType<BlockStatisticTurnstile.BlockEntityStatisticTurnstile>>
			BLOCK_ENTITY_TYPE_STATISTIC_TURNSTILE = new RegistryObject<>(() ->
			RegistryUtilities.getBlockEntityType(
					BlockStatisticTurnstile.BlockEntityStatisticTurnstile::new,
					BLOCK_STATISTIC_TURNSTILE.get()
			));
	public static final RegistryObject<Block> BLOCK_FEEDBACK_BOX = new RegistryObject<>(BlockFeedbackBox::new);
	public static final RegistryObject<BlockEntityType<BlockFeedbackBox.BlockEntityFeedbackBox>>
			BLOCK_ENTITY_TYPE_FEEDBACK_BOX = new RegistryObject<>(() ->
			RegistryUtilities.getBlockEntityType(
					BlockFeedbackBox.BlockEntityFeedbackBox::new,
					BLOCK_FEEDBACK_BOX.get()
			));

	public static final SoundEvent SOUND_EVENT_BELL = new SoundEvent(new ResourceLocation("mtrsteamloco:bell"));

	public static SimpleParticleType PARTICLE_STEAM_SMOKE;

	public static void init(
			RegisterBlockItem registerBlockItem,
			BiConsumer<String,RegistryObject<? extends BlockEntityType<? extends BlockEntityMapper>>> registerBlockEntityType,
			BiConsumer<String, SoundEvent> registerSoundEvent
	) {
		registerBlockItem.accept("departure_bell", BLOCK_DEPARTURE_BELL, ItemGroups.RAILWAY_FACILITIES);
		registerBlockItem.accept("statistic_turnstile", BLOCK_STATISTIC_TURNSTILE, ItemGroups.RAILWAY_FACILITIES);
		registerBlockEntityType.accept("statistic_turnstile", BLOCK_ENTITY_TYPE_STATISTIC_TURNSTILE);
		registerBlockItem.accept("feedback_box", BLOCK_FEEDBACK_BOX, ItemGroups.RAILWAY_FACILITIES);
		registerBlockEntityType.accept("feedback_box", BLOCK_ENTITY_TYPE_FEEDBACK_BOX);
		registerSoundEvent.accept("bell", SOUND_EVENT_BELL);

		mtr.Registry.registerServerStartingEvent(minecraftServer -> {
			ServerConfig.load(minecraftServer);
		});
		mtr.Registry.registerNetworkReceiver(PacketFeedback.PACKET_FEEDBACK, PacketFeedback::receiveFeedbackC2S);
	}

	@FunctionalInterface
	public interface RegisterBlockItem {
		void accept(String string, RegistryObject<Block> block, CreativeModeTab tab);
	}
}
