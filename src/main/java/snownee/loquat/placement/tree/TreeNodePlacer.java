package snownee.loquat.placement.tree;

import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.JigsawBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import snownee.loquat.Loquat;
import snownee.loquat.duck.LoquatStructurePiece;
import snownee.loquat.placement.LoquatPlacer;

public class TreeNodePlacer implements LoquatPlacer {

	private final String structureIdPattern;
	private final BuildTreeFunction buildTreeFunction;

	public TreeNodePlacer(String structureIdPattern, BuildTreeFunction buildTreeFunction) {
		this.structureIdPattern = structureIdPattern;
		this.buildTreeFunction = buildTreeFunction;
	}

	@Override
	public boolean accept(ResourceLocation structureId) {
		return structureId.toString().matches(structureIdPattern);
	}

	@Override
	public Structure.GenerationStub place(ResourceLocation structureId, RandomState randomState, BlockPos defaultStartPos, VoxelShape defaultValidSpace, int range, ChunkGenerator chunkGenerator, StructureTemplateManager structureTemplateManager, LevelHeightAccessor levelHeightAccessor, WorldgenRandom worldgenRandom, Registry<StructureTemplatePool> pools, PoolElementStructurePiece defaultStartPiece) {
		return new Structure.GenerationStub(defaultStartPos, structurePiecesBuilder -> {
			try {
				TreeNode root = new TreeNode(new ResourceLocation("start"), null);
				RandomSource random = RandomSource.create();
				root = buildTreeFunction.buildTree(random, root);
				SetMultimap<String, ResourceLocation> uniqueGroups = HashMultimap.create();
				Preconditions.checkState(root.getUniqueGroup() == null, "Root node must not have unique group");
				Stack<Step> steps = new Stack<>();
				steps.push(new Step(defaultStartPiece, defaultValidSpace, defaultStartPos, root));
				doPlace(root, uniqueGroups, steps, structureTemplateManager, random, pools);
				for (var step : steps) {
					if (step.node.tags.size() > 0 || step.node.getData() != null) {
						CompoundTag data = new CompoundTag();
						if (step.node.tags.size() > 0) {
							ListTag tags = new ListTag();
							for (var tag : step.node.tags) {
								tags.add(StringTag.valueOf(tag));
							}
							data.put("Tags", tags);
						}
						if (step.node.getData() != null) {
							data.put("Data", step.node.getData());
						}
						((LoquatStructurePiece) step.piece).loquat$setAttachedData(data);
					}
					structurePiecesBuilder.addPiece(step.piece);
				}
			} catch (Throwable e) {
				Loquat.LOGGER.error("", e);
			}
		});
	}

	private boolean doPlace(TreeNode node, SetMultimap<String, ResourceLocation> uniqueGroups, Stack<Step> steps, StructureTemplateManager structureTemplateManager, RandomSource random, Registry<StructureTemplatePool> pools) {
		Step step = steps.peek();
		StructurePoolElement element = step.piece.getElement();
		BlockPos blockPos = step.piece.getPosition();
		Rotation rotation = step.piece.getRotation();
		List<StructureTemplate.StructureBlockInfo> jigsawBlocks = element.getShuffledJigsawBlocks(structureTemplateManager, BlockPos.ZERO, rotation, random);
		ListMultimap<String, StructureTemplate.StructureBlockInfo> byJointName = ArrayListMultimap.create();
		Set<StructureTemplate.StructureBlockInfo> resolvedJigsaws = Sets.newHashSet();
		for (var jigsawBlock : jigsawBlocks) {
			if (step.jointPos.equals(jigsawBlock.pos.offset(blockPos))) {
				resolvedJigsaws.add(jigsawBlock);
				continue;
			}
			String jointName = jigsawBlock.nbt.getString("name");
			byJointName.put(jointName, jigsawBlock);
		}
		for (TreeNode child : node.getChildren()) {
			String jointName = child.getParentEdge();
			StructureTemplate.StructureBlockInfo selectedJigsaw = null;
			joints:
			for (var jigsawBlock : byJointName.get(jointName)) {
				int minEdgeDistance = node.getMinEdgeDistance();
				BlockPos jointPos = blockPos.offset(jigsawBlock.pos);
				Direction direction = JigsawBlock.getFrontFacing(jigsawBlock.state);
				BlockPos thatJointPos = jointPos.relative(direction);
				double dist = step.jointPos.distSqr(jointPos);
				if (dist <= minEdgeDistance * minEdgeDistance) {
					continue;
				}
				StructureTemplatePool pool = pools.getOptional(child.getPool()).orElseThrow();
				for (StructurePoolElement template : pool.getShuffledTemplates(random)) {
					for (Rotation rotation2 : Rotation.getShuffled(random)) {
						List<StructureTemplate.StructureBlockInfo> jigsawBlocks1 = template.getShuffledJigsawBlocks(structureTemplateManager, BlockPos.ZERO, rotation2, random);
						for (StructureTemplate.StructureBlockInfo jigsawBlock1 : jigsawBlocks1) {
							if (!JigsawBlock.canAttach(jigsawBlock, jigsawBlock1)) {
								continue;
							}
							BlockPos thatPiecePos = thatJointPos.offset(jigsawBlock1.pos.multiply(-1));
							BoundingBox boundingBox = template.getBoundingBox(structureTemplateManager, thatPiecePos, rotation2);
							PoolElementStructurePiece piece = new PoolElementStructurePiece(structureTemplateManager, template, thatPiecePos, 0, rotation2, boundingBox);
							VoxelShape validSpace = steps.peek().validSpace;
							if (Shapes.joinIsNotEmpty(validSpace, Shapes.create(AABB.of(boundingBox).deflate(0.25)), BooleanOp.ONLY_SECOND)) {
								continue;
							}
							VoxelShape shape = Shapes.joinUnoptimized(validSpace, Shapes.create(AABB.of(boundingBox)), BooleanOp.ONLY_FIRST);
							steps.push(new Step(piece, shape, thatJointPos, child));
							if (doPlace(child, uniqueGroups, steps, structureTemplateManager, random, pools)) {
								resolvedJigsaws.add(jigsawBlock);
								selectedJigsaw = jigsawBlock;
								break joints;
							}
						}
					}
				}
			}
			if (selectedJigsaw == null) {
				while (steps.peek() != step) {
					steps.pop();
				}
				steps.pop();
				return false;
			}
		}
		// Find fallbacks
		for (var jigsawBlock : jigsawBlocks) {
			if (resolvedJigsaws.contains(jigsawBlock)) {
				continue;
			}
			String jointName = jigsawBlock.nbt.getString("name");
			TreeNode fallbackNode = node.getFallbackNodeProvider().apply(jointName);
			if (fallbackNode == null) {
				continue;
			}
			BlockPos jointPos = blockPos.offset(jigsawBlock.pos);
			StructureTemplatePool pool = pools.getOptional(fallbackNode.getPool()).orElseThrow();
			StructurePoolElement template = pool.getRandomTemplate(random);
			for (Rotation rotation2 : Rotation.getShuffled(random)) {
				List<StructureTemplate.StructureBlockInfo> jigsawBlocks1 = template.getShuffledJigsawBlocks(structureTemplateManager, BlockPos.ZERO, rotation2, random);
				for (StructureTemplate.StructureBlockInfo jigsawBlock1 : jigsawBlocks1) {
					if (!JigsawBlock.canAttach(jigsawBlock, jigsawBlock1)) {
						continue;
					}
					BlockPos thatPiecePos = jointPos.offset(jigsawBlock1.pos.multiply(-1));
					BoundingBox boundingBox = template.getBoundingBox(structureTemplateManager, thatPiecePos, rotation2);
					PoolElementStructurePiece piece = new PoolElementStructurePiece(structureTemplateManager, template, thatPiecePos, 0, rotation2, boundingBox);
					VoxelShape shape = Shapes.joinUnoptimized(steps.peek().validSpace, Shapes.create(AABB.of(boundingBox)), BooleanOp.ONLY_FIRST);
					steps.push(new Step(piece, shape, jointPos, fallbackNode));
				}
			}
		}
		return true;
	}

	private record Step(PoolElementStructurePiece piece, VoxelShape validSpace, BlockPos jointPos, TreeNode node) {
	}

}
