package snownee.loquat.spawner;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import snownee.loquat.Loquat;
import snownee.loquat.spawner.difficulty.DifficultyLoader;
import snownee.loquat.util.CommonProxy;
import snownee.lychee.PostActionTypes;
import snownee.lychee.mixin.LootContextParamSetsAccess;

public class LycheeCompat {
	public static final ResourceLocation DIFFICULTY_ID = new ResourceLocation(Loquat.ID, "difficulty");
	public static final ResourceLocation SPAWNER_ID = new ResourceLocation(Loquat.ID, "spawner");
	public static final LootContextParamSet LOOT_CONTEXT_PARAM_SET = LootContextParamSetsAccess.callRegister(SPAWNER_ID.toString(), $ -> {
		$.required(LootContextParams.ORIGIN);
	});

	public static void init() {
		CommonProxy.registerReloadListener(DifficultyLoader.INSTANCE);
		CommonProxy.registerReloadListener(SpawnerLoader.INSTANCE);
		PostActionTypes.register("loquat:spawn", SpawnMobAction.TYPE);
	}

}
