package snownee.loquat.compat.kubejs;

import dev.latvian.mods.kubejs.player.SimplePlayerEventJS;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import snownee.loquat.core.AreaManager;
import snownee.loquat.core.area.Area;

public class PlayerAreaEventJS extends SimplePlayerEventJS {

	private final Area area;

	public PlayerAreaEventJS(Player p, Area area) {
		super(p);
		this.area = area;
	}

	public Area getArea() {
		return area;
	}

	public AreaManager getAreaManager() {
		if (getLevel() instanceof ServerLevel serverLevel) {
			return AreaManager.of(serverLevel);
		}
		return null;
	}
}
