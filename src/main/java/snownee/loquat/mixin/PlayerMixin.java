package snownee.loquat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import snownee.loquat.core.AreaManager;
import snownee.loquat.core.RestrictInstance;
import snownee.loquat.core.select.SelectionManager;
import snownee.loquat.duck.LoquatPlayer;

@Mixin(Player.class)
public class PlayerMixin implements LoquatPlayer {

	private SelectionManager loquat$selection;

	@Override
	public SelectionManager loquat$getSelectionManager() {
		if (loquat$selection == null) {
			loquat$selection = new SelectionManager(((Entity) (Object) this).level.isClientSide);
		}
		return loquat$selection;
	}

}
