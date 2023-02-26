package snownee.loquat.core.select;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;

import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.StructureBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import snownee.loquat.LoquatConfig;
import snownee.loquat.core.AreaManager;
import snownee.loquat.core.area.Area;
import snownee.loquat.network.SSyncSelectionPacket;

public class SelectionManager {

	public static SelectionManager of(Player player) {
		return ((LoquatPlayer) player).loquat$getSelectionManager();
	}

	@Getter
	private final List<PosSelection> selections;
	@Getter
	private boolean lastOneIncomplete;
	@Getter
	private final List<UUID> selectedAreas;

	public SelectionManager(boolean isClientSide) {
		selections = isClientSide ? Collections.synchronizedList(Lists.newArrayList()) : Lists.newArrayList();
		selectedAreas = isClientSide ? Collections.synchronizedList(Lists.newArrayList()) : Lists.newArrayList();
	}

	public boolean leftClickBlock(ServerLevel world, BlockPos pos, ServerPlayer player) {
		if (!isHoldingTool(player))
			return false;
		if (player.isShiftKeyDown()) {
			AreaManager manager = AreaManager.of(world);
			for (Area area : manager.areas()) {
				if (!area.contains(pos))
					continue;
				if (selectedAreas.contains(area.getUuid())) {
					selectedAreas.remove(area.getUuid());
				} else {
					selectedAreas.add(area.getUuid());
				}
			}
		} else if (world.getBlockEntity(pos) instanceof StructureBlockEntity be) {
			if (selections.size() == 1) {
				AABB aabb = selections.get(0).toAABB();
				be.setStructurePos(new BlockPos(aabb.minX, aabb.minY, aabb.minZ));
				be.setStructureSize(new BlockPos(aabb.getXsize(), aabb.getYsize(), aabb.getZsize()));
			}
			be.setShowBoundingBox(true);
			be.setChanged();
			BlockState blockState = world.getBlockState(pos);
			world.sendBlockUpdated(pos, blockState, blockState, 3);
		} else {
			if (selections.isEmpty()) {
				lastOneIncomplete = false;
			}
			if (lastOneIncomplete) {
				selections.get(selections.size() - 1).pos2 = pos;
			} else {
				selections.add(new PosSelection(pos));
			}
			lastOneIncomplete = !lastOneIncomplete;
		}
		SSyncSelectionPacket.sync(player);
		return true;
	}

	public boolean rightClickItem(ServerLevel world, BlockPos pos, ServerPlayer player) {
		if (!isHoldingTool(player) || !player.isShiftKeyDown())
			return false;
		selections.clear();
		SSyncSelectionPacket.sync(player);
		return true;
	}

	public boolean isSelected(Area area) {
		return selectedAreas.contains(area.getUuid());
	}

	public static boolean isHoldingTool(Player player) {
		return player.hasPermissions(2) && player.isCreative() && player.getMainHandItem().is(LoquatConfig.selectionItem);
	}

	public static boolean removeInvalidAreas(ServerPlayer player) {
		AreaManager areaManager = AreaManager.of((ServerLevel) player.level);
		return SelectionManager.of(player).getSelectedAreas().removeIf(uuid -> areaManager.get(uuid) == null);
	}

}
