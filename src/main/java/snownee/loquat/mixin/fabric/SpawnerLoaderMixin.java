package snownee.loquat.mixin.fabric;

import java.util.Collection;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import snownee.loquat.spawner.LycheeCompat;
import snownee.loquat.spawner.SpawnerLoader;

@Mixin(value = SpawnerLoader.class, remap = false)
public abstract class SpawnerLoaderMixin implements IdentifiableResourceReloadListener {
	@Override
	public ResourceLocation getFabricId() {
		return LycheeCompat.SPAWNER_ID;
	}

	@Override
	public Collection<ResourceLocation> getFabricDependencies() {
		return List.of(LycheeCompat.DIFFICULTY_ID);
	}
}
