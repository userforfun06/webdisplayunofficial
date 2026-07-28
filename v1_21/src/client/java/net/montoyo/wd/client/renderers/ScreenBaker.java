/*
 * Copyright (C) 2018 BARBOTIN Nicolas
 */

package net.montoyo.wd.client.renderers;

import com.google.common.collect.ImmutableList;
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.RandomSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.montoyo.wd.utilities.data.BlockSide;
import net.montoyo.wd.utilities.math.Vector3f;
import net.montoyo.wd.utilities.math.Vector3i;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public class ScreenBaker implements BakedModel {

	private static final List<BakedQuad> noQuads = ImmutableList.of();
	private final TextureAtlasSprite[] texs = new TextureAtlasSprite[16];
	private final BlockSide[] blockSides = BlockSide.values();
	private final Direction[] blockFacings = Direction.values();

	public ScreenBaker(Function<net.minecraft.client.resources.model.Material, TextureAtlasSprite> spriteGetter) {
		for (int i = 0; i < texs.length; i++) {
			texs[i] = spriteGetter.apply(ScreenModelLoader.MATERIALS_SIDES[i]);
		}
	}

	@Override
	public boolean isVanillaAdapter() {
		return false;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random) {
		return noQuads;
	}

	private static Vector3f rotateVec(float x, float z, BlockSide side) {
		return switch (side) {
			case BOTTOM -> new Vector3f(x, 1.0f, 1.0f - z);
			case TOP -> new Vector3f(x, 0.0f, z);
			case NORTH -> new Vector3f(x, z, 1.0f);
			case SOUTH -> new Vector3f(x, 1.0f - z, 0.0f);
			case WEST -> new Vector3f(1.0f, x, z);
			case EAST -> new Vector3f(0.0f, 1.0f - x, z);
		};
	}

	private static Vector3f rotateTex(BlockSide side, float u, float v) {
		return switch (side) {
			case BOTTOM, NORTH -> new Vector3f(16.0f - u, 16.0f - v, 0.0f);
			case TOP -> new Vector3f(16.0f - u, v, 0.0f);
			case SOUTH -> new Vector3f(u, v, 0.0f);
			case WEST -> new Vector3f(16.0f - v, u, 0.0f);
			case EAST -> new Vector3f(v, 16.0f - u, 0.0f);
		};
	}

	@Override
	public void emitBlockQuads(BlockAndTintGetter level, BlockState state, BlockPos pos,
							   Supplier<RandomSource> randomSupplier, RenderContext context) {
		final int BAR_BOTTOM = 1;
		final int BAR_RIGHT  = 2;
		final int BAR_TOP    = 4;
		final int BAR_LEFT   = 8;

		// Input vertex positions and UVs (matching 1.20's bakeSide verbatim)
		float[][] inputPos = {
			{0.0f, 0.0f, 0.0f},
			{0.0f, 0.0f, 1.0f},
			{1.0f, 0.0f, 1.0f},
			{1.0f, 0.0f, 0.0f}
		};
		float[][] inputUv = {
			{16.0f, 0.0f},
			{16.0f, 16.0f},
			{0.0f, 16.0f},
			{0.0f, 0.0f}
		};

		for (int i = 0; i < blockSides.length; i++) {
			// Texture computed with direct BlockSide (matching 1.20's getModelData)
			BlockSide side = blockSides[i];
			int res = switch (check(state, level, pos, side.up)) {
				case 1 -> BAR_BOTTOM;
				case 2 -> BAR_TOP;
				case 3 -> BAR_TOP | BAR_BOTTOM;
				default -> 0;
			};
			res |= switch (check(state, level, pos, side.right)) {
				case 1 -> BAR_LEFT;
				case 2 -> BAR_RIGHT;
				case 3 -> BAR_LEFT | BAR_RIGHT;
				default -> 0;
			};

			// Quad baked with reversed BlockSide (matching 1.20's bakeSide)
			BlockSide bakeS = blockSides[BlockSide.reverse(i)];
			int rotation = switch (bakeS) {
				case NORTH, TOP, BOTTOM -> 2;
				case SOUTH -> 0;
				case EAST -> 1;
				case WEST -> 3;
			};

			TextureAtlasSprite tex = texs[res];
			Direction face = blockFacings[BlockSide.reverse(i)].getOpposite();

			var emitter = context.getEmitter();
			emitter.cullFace(face);
			emitter.nominalFace(face);

			for (int v = 0; v < 4; v++) {
				int idx = switch (v) {
					case 0 -> (rotation + 3) % 4;
					case 1 -> (rotation + 2) % 4;
					case 2 -> (rotation + 1) % 4;
					default -> rotation % 4;
				};

				Vector3f p = rotateVec(inputPos[idx][0], inputPos[idx][2], bakeS);
				emitter.pos(v, p.x, p.y, p.z);

				Vector3f uv = rotateTex(bakeS, inputUv[idx][0], inputUv[idx][1]);
				emitter.uv(v, uv.x / 16.0f, uv.y / 16.0f);
			}

			emitter.spriteBake(tex, MutableQuadView.BAKE_NORMALIZED);
			emitter.color(-1, -1, -1, -1);
			emitter.emit();
		}
	}

	@Override
	public void emitItemQuads(ItemStack stack, Supplier<RandomSource> randomSupplier,
							  RenderContext context) {
		for (int i = 0; i < blockSides.length; i++) {
			context.getEmitter()
				.square(blockFacings[i], 0, 0, 1, 1, 0)
				.spriteBake(texs[15], MutableQuadView.BAKE_LOCK_UV)
				.color(-1, -1, -1, -1)
				.emit();
		}
	}

	protected byte check(BlockState state, BlockAndTintGetter level, BlockPos pos, Vector3i dir) {
		BlockState u = level.getBlockState(pos.offset(dir.x, dir.y, dir.z));
		BlockState d = level.getBlockState(pos.offset(-dir.x, -dir.y, -dir.z));
		if (
				u.getBlock() == state.getBlock() &&
						d.getBlock() != state.getBlock()
		) return (byte) 1; // away
		else if (
				d.getBlock() == state.getBlock() &&
						u.getBlock() != state.getBlock()
		) return (byte) 2; // to
		else if (
				d.getBlock() != state.getBlock() &&
						u.getBlock() != state.getBlock()
		) return (byte) 3; // both
		return (byte) 0; // none
	}

	@Override
	public boolean useAmbientOcclusion() {
		return false;
	}

	@Override
	public boolean isGui3d() {
		return true;
	}

	@Override
	public boolean usesBlockLight() {
		return false;
	}

	@Override
	public boolean isCustomRenderer() {
		return false;
	}

	@Override
	@NotNull
	public TextureAtlasSprite getParticleIcon() {
		return texs[15];
	}

	@Override
	@NotNull
	public ItemTransforms getTransforms() {
		return new ItemTransforms(
			new ItemTransform(new org.joml.Vector3f(75, 45, 0), new org.joml.Vector3f(0, 0.25f, 0), new org.joml.Vector3f(0.375f, 0.375f, 0.375f)),
			new ItemTransform(new org.joml.Vector3f(75, 45, 0), new org.joml.Vector3f(0, 0.25f, 0), new org.joml.Vector3f(0.375f, 0.375f, 0.375f)),
			new ItemTransform(new org.joml.Vector3f(0, 45, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.4f, 0.4f, 0.4f)),
			new ItemTransform(new org.joml.Vector3f(0, 45, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.4f, 0.4f, 0.4f)),
			ItemTransform.NO_TRANSFORM,
			new ItemTransform(new org.joml.Vector3f(30, 225, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.625f, 0.625f, 0.625f)),
			new ItemTransform(new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0, 0.15f, 0), new org.joml.Vector3f(0.25f, 0.25f, 0.25f)),
			new ItemTransform(new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.5f, 0.5f, 0.5f))
		);
	}

	@Override
	@NotNull
	public ItemOverrides getOverrides() {
		return ItemOverrides.EMPTY;
	}
}
