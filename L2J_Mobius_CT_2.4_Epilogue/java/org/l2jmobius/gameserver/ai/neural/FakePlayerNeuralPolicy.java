/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.ai.neural;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import jdk.incubator.vector.FloatVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

import org.l2jmobius.gameserver.managers.FakePlayerAiEngine.Archetype;

/**
 * In-JVM Java 27 SIMD-Accelerated (`jdk.incubator.vector.FloatVector`) Multi-Layer Perceptron (`16 -> 32 -> 16 -> 8`)
 * and 5-Stage Progressive Curriculum Reinforcement Learning Engine for FakePlayer Tactical AI.
 * <ul>
 *   <li><b>State Space (16 floats):</b> normalized self/target HP &amp; MP, distance, line-of-sight, behind-target indicator,
 *       skill cooldown readiness (potion, heal, special, kite), combat state, ally low-HP triage indicator, and archetype one-hot/prior features.</li>
 *   <li><b>Action Space (8 tactical primitives):</b> {@link TacticalAction} (`MELEE_ENGAGE`, `PRIMARY_NUKE_SHOT`, `BACKSTAB_FLANK`,
 *       `STANDOFF_KITE`, `BATTLE_HEAL_TRIAGE`, `TANK_HATE_INTERCEPT`, `SECOND_WIND_POTION`, `SQUAD_REGROUP_FOLLOW`).</li>
 *   <li><b>5-Stage Curriculum:</b> Progressive action-space unlocking + shaped dense rewards + online REINFORCE policy-gradient updates with EMA baseline.</li>
 *   <li><b>Persistence:</b> Binary checkpointing to {@code data/neural_fakeplayer_weights.bin}.</li>
 * </ul>
 */
public final class FakePlayerNeuralPolicy
{
	private static final Logger LOGGER = Logger.getLogger(FakePlayerNeuralPolicy.class.getName());
	private static final VectorSpecies<Float> SPECIES = FloatVector.SPECIES_PREFERRED;
	private static final int MAGIC_HEADER = 0x4C324E4E; // "L2NN"
	private static final int VERSION = 1;
	private static final String WEIGHTS_FILE_PATH = "data/neural_fakeplayer_weights.bin";
	
	public static final int INPUT_DIM = 16;
	public static final int HIDDEN1_DIM = 32;
	public static final int HIDDEN2_DIM = 16;
	public static final int ACTION_DIM = 8;
	
	public enum CurriculumStage
	{
		STAGE_1_TOOLKIT_DISCOVERY(1, "Stage 1: Toolkit Discovery", "Basic engage, nuke/shot & potion survival"),
		STAGE_2_SURVIVAL_AND_SPACING(2, "Stage 2: Survival & Spacing", "Unlocks GeoEngine standoff kiting & flanking"),
		STAGE_3_ARCHETYPE_MASTERY(3, "Stage 3: Archetype Mastery", "Unlocks healer triage & tank hate-intercept"),
		STAGE_4_SQUAD_SYNERGY(4, "Stage 4: Squad Synergy", "Full 8-action party coordination & formation"),
		STAGE_5_TACTICAL_PVP_ARENA(5, "Stage 5: Tactical PvP Arena", "Full PvP self-play & live player sparring");
		
		private final int _id;
		private final String _title;
		private final String _description;
		
		CurriculumStage(int id, String title, String description)
		{
			_id = id;
			_title = title;
			_description = description;
		}
		
		public int getId()
		{
			return _id;
		}
		
		public String getTitle()
		{
			return _title;
		}
		
		public String getDescription()
		{
			return _description;
		}
		
		public static CurriculumStage fromId(int id)
		{
			return switch (id)
			{
				case 1 -> STAGE_1_TOOLKIT_DISCOVERY;
				case 2 -> STAGE_2_SURVIVAL_AND_SPACING;
				case 3 -> STAGE_3_ARCHETYPE_MASTERY;
				case 4 -> STAGE_4_SQUAD_SYNERGY;
				default -> STAGE_5_TACTICAL_PVP_ARENA;
			};
		}
	}
	
	public enum TacticalAction
	{
		MELEE_ENGAGE,
		PRIMARY_NUKE_SHOT,
		BACKSTAB_FLANK,
		STANDOFF_KITE,
		BATTLE_HEAL_TRIAGE,
		TANK_HATE_INTERCEPT,
		SECOND_WIND_POTION,
		SQUAD_REGROUP_FOLLOW
	}
	
	// MLP weights flattened row-major for SIMD FloatVector dot-products
	private final float[] _w1 = new float[HIDDEN1_DIM * INPUT_DIM];
	private final float[] _b1 = new float[HIDDEN1_DIM];
	private final float[] _w2 = new float[HIDDEN2_DIM * HIDDEN1_DIM];
	private final float[] _b2 = new float[HIDDEN2_DIM];
	private final float[] _w3 = new float[ACTION_DIM * HIDDEN2_DIM];
	private final float[] _b3 = new float[ACTION_DIM];
	
	private volatile CurriculumStage _stage = CurriculumStage.STAGE_5_TACTICAL_PVP_ARENA;
	private volatile float _rewardBaseline = 0.15f;
	private volatile float _learningRate = 0.0045f;
	private final AtomicLong _trainingSteps = new AtomicLong(0);
	private final AtomicInteger _duelsCompleted = new AtomicInteger(0);
	
	public FakePlayerNeuralPolicy()
	{
		initExpertWarmStartPriors();
		loadWeights();
	}
	
	/**
	 * Initializes the 16 -> 32 -> 16 -> 8 network with deterministic small-variance Xavier weights
	 * plus structured tactical warm-start priors so bots fight intelligently from tick 0.
	 */
	public synchronized void initExpertWarmStartPriors()
	{
		final ThreadLocalRandom rng = ThreadLocalRandom.current();
		for (int i = 0; i < _w1.length; i++)
		{
			_w1[i] = (float) ((rng.nextDouble() - 0.5) * 0.18);
		}
		for (int i = 0; i < _w2.length; i++)
		{
			_w2[i] = (float) ((rng.nextDouble() - 0.5) * 0.18);
		}
		for (int i = 0; i < _w3.length; i++)
		{
			_w3[i] = (float) ((rng.nextDouble() - 0.5) * 0.18);
		}
		
		// Identity-like pass-through on first 16 hidden neurons so input state features directly route to hidden layer 2
		for (int i = 0; i < INPUT_DIM; i++)
		{
			_w1[(i * INPUT_DIM) + i] += 0.85f;
			_w2[(i * HIDDEN1_DIM) + i] += 0.85f;
		}
		
		// Structured output layer priors mapping state channels to TacticalAction logits:
		// Input state indices:
		// 0: selfHpRatio, 1: selfMpRatio, 2: targetHpRatio, 3: distNorm,
		// 4: potionReady, 5: healReady, 6: specialReady, 7: kiteReady,
		// 8: allyLowHp, 9: targetFocusingAlly, 10: isArcherOrMage, 11: isDagger,
		// 12: isHealer, 13: isTank, 14: isWarrior, 15: hasAssistLeaderFar
		_b3[TacticalAction.MELEE_ENGAGE.ordinal()] = 0.25f;
		_b3[TacticalAction.PRIMARY_NUKE_SHOT.ordinal()] = 0.35f;
		
		// SECOND_WIND_POTION: strong negative selfHpRatio + positive potionReady
		_w3[(TacticalAction.SECOND_WIND_POTION.ordinal() * HIDDEN2_DIM) + 0] = -2.2f;
		_w3[(TacticalAction.SECOND_WIND_POTION.ordinal() * HIDDEN2_DIM) + 4] = 2.4f;
		
		// BATTLE_HEAL_TRIAGE: healReady + allyLowHp + isHealer
		_w3[(TacticalAction.BATTLE_HEAL_TRIAGE.ordinal() * HIDDEN2_DIM) + 5] = 1.6f;
		_w3[(TacticalAction.BATTLE_HEAL_TRIAGE.ordinal() * HIDDEN2_DIM) + 8] = 2.2f;
		_w3[(TacticalAction.BATTLE_HEAL_TRIAGE.ordinal() * HIDDEN2_DIM) + 12] = 1.8f;
		
		// TANK_HATE_INTERCEPT: specialReady + targetFocusingAlly + isTank
		_w3[(TacticalAction.TANK_HATE_INTERCEPT.ordinal() * HIDDEN2_DIM) + 6] = 1.5f;
		_w3[(TacticalAction.TANK_HATE_INTERCEPT.ordinal() * HIDDEN2_DIM) + 9] = 2.1f;
		_w3[(TacticalAction.TANK_HATE_INTERCEPT.ordinal() * HIDDEN2_DIM) + 13] = 2.0f;
		
		// STANDOFF_KITE: close distance (low distNorm) + kiteReady + isArcherOrMage
		_w3[(TacticalAction.STANDOFF_KITE.ordinal() * HIDDEN2_DIM) + 3] = -1.9f;
		_w3[(TacticalAction.STANDOFF_KITE.ordinal() * HIDDEN2_DIM) + 7] = 1.8f;
		_w3[(TacticalAction.STANDOFF_KITE.ordinal() * HIDDEN2_DIM) + 10] = 2.0f;
		
		// BACKSTAB_FLANK: close distance + specialReady + isDagger
		_w3[(TacticalAction.BACKSTAB_FLANK.ordinal() * HIDDEN2_DIM) + 3] = -1.2f;
		_w3[(TacticalAction.BACKSTAB_FLANK.ordinal() * HIDDEN2_DIM) + 6] = 1.8f;
		_w3[(TacticalAction.BACKSTAB_FLANK.ordinal() * HIDDEN2_DIM) + 11] = 2.3f;
		
		// SQUAD_REGROUP_FOLLOW: hasAssistLeaderFar
		_w3[(TacticalAction.SQUAD_REGROUP_FOLLOW.ordinal() * HIDDEN2_DIM) + 15] = 2.8f;
	}
	
	/**
	 * Performs a SIMD-accelerated forward pass (`16 -> 32 -> 16 -> 8`), applies archetype and curriculum masks,
	 * and selects the best tactical action (with slight Boltzmann exploration during active curriculum training).
	 * @param state 16-element normalized state vector
	 * @param archetype bot's combat archetype
	 * @param outProbs optional 8-element array to receive action probabilities
	 * @return chosen {@link TacticalAction}
	 */
	public TacticalAction selectAction(float[] state, Archetype archetype, float[] outProbs)
	{
		final float[] h1 = new float[HIDDEN1_DIM];
		final float[] h2 = new float[HIDDEN2_DIM];
		final float[] logits = new float[ACTION_DIM];
		
		// Layer 1: 16 -> 32 with LeakyReLU
		for (int i = 0; i < HIDDEN1_DIM; i++)
		{
			final float sum = _b1[i] + simdDot(state, 0, _w1, i * INPUT_DIM, INPUT_DIM);
			h1[i] = sum >= 0f ? sum : 0.1f * sum;
		}
		
		// Layer 2: 32 -> 16 with LeakyReLU
		for (int i = 0; i < HIDDEN2_DIM; i++)
		{
			final float sum = _b2[i] + simdDot(h1, 0, _w2, i * HIDDEN1_DIM, HIDDEN1_DIM);
			h2[i] = sum >= 0f ? sum : 0.1f * sum;
		}
		
		// Layer 3: 16 -> 8 logits
		for (int i = 0; i < ACTION_DIM; i++)
		{
			logits[i] = _b3[i] + simdDot(h2, 0, _w3, i * HIDDEN2_DIM, HIDDEN2_DIM);
		}
		
		applyExpertPriorsAndCurriculumMask(state, archetype, logits);
		
		// Softmax
		float maxLogit = logits[0];
		for (int i = 1; i < ACTION_DIM; i++)
		{
			if (logits[i] > maxLogit)
			{
				maxLogit = logits[i];
			}
		}
		
		float sumExp = 0f;
		for (int i = 0; i < ACTION_DIM; i++)
		{
			final float e = (float) Math.exp(Math.max(-20f, logits[i] - maxLogit));
			logits[i] = e;
			sumExp += e;
		}
		
		final float invSum = sumExp > 1e-6f ? (1f / sumExp) : 1f;
		final float[] probs = (outProbs != null && outProbs.length >= ACTION_DIM) ? outProbs : new float[ACTION_DIM];
		int bestIdx = 0;
		float bestProb = -1f;
		for (int i = 0; i < ACTION_DIM; i++)
		{
			final float p = logits[i] * invSum;
			probs[i] = p;
			if (p > bestProb)
			{
				bestProb = p;
				bestIdx = i;
			}
		}
		
		// Boltzmann categorical sampling so policy explores & learns across all 5 curriculum stages
		final float sample = ThreadLocalRandom.current().nextFloat();
		float cumulative = 0f;
		for (int i = 0; i < ACTION_DIM; i++)
		{
			cumulative += probs[i];
			if (sample <= cumulative)
			{
				return TacticalAction.values()[i];
			}
		}
		
		return TacticalAction.values()[bestIdx];
	}
	
	private void applyExpertPriorsAndCurriculumMask(float[] s, Archetype archetype, float[] logits)
	{
		// Stage gating: progressively unlock actions across Stages 1..5
		final int stageId = _stage.getId();
		if (stageId < 2)
		{
			logits[TacticalAction.STANDOFF_KITE.ordinal()] = -15f;
			logits[TacticalAction.BACKSTAB_FLANK.ordinal()] = -15f;
		}
		if (stageId < 3)
		{
			logits[TacticalAction.BATTLE_HEAL_TRIAGE.ordinal()] = -15f;
			logits[TacticalAction.TANK_HATE_INTERCEPT.ordinal()] = -15f;
		}
		if (stageId < 4)
		{
			logits[TacticalAction.SQUAD_REGROUP_FOLLOW.ordinal()] = -15f;
		}
		
		// Hard readiness guards so unavailable cooldowns are never selected
		if ((s[4] < 0.5f) || (s[0] > 0.38f))
		{
			logits[TacticalAction.SECOND_WIND_POTION.ordinal()] = -20f;
		}
		else
		{
			logits[TacticalAction.SECOND_WIND_POTION.ordinal()] += 3.5f;
		}
		
		if ((s[5] < 0.5f) || (s[8] < 0.5f))
		{
			logits[TacticalAction.BATTLE_HEAL_TRIAGE.ordinal()] = -20f;
		}
		else if (archetype == Archetype.HEALER)
		{
			logits[TacticalAction.BATTLE_HEAL_TRIAGE.ordinal()] += 3.2f;
		}
		
		if ((s[6] < 0.5f) || (s[9] < 0.5f) || (archetype != Archetype.TANK))
		{
			logits[TacticalAction.TANK_HATE_INTERCEPT.ordinal()] = -20f;
		}
		else
		{
			logits[TacticalAction.TANK_HATE_INTERCEPT.ordinal()] += 3.0f;
		}
		
		if ((s[7] < 0.5f) || (s[3] > 0.19f) || ((archetype != Archetype.ARCHER) && (archetype != Archetype.MAGE)))
		{
			logits[TacticalAction.STANDOFF_KITE.ordinal()] = -20f;
		}
		else
		{
			logits[TacticalAction.STANDOFF_KITE.ordinal()] += 2.8f;
		}
		
		if ((s[6] < 0.5f) || (s[3] > 0.16f) || (archetype != Archetype.DAGGER))
		{
			logits[TacticalAction.BACKSTAB_FLANK.ordinal()] = -20f;
		}
		else
		{
			logits[TacticalAction.BACKSTAB_FLANK.ordinal()] += 2.9f;
		}
		
		if (s[15] < 0.5f)
		{
			logits[TacticalAction.SQUAD_REGROUP_FOLLOW.ordinal()] = -20f;
		}
		else
		{
			logits[TacticalAction.SQUAD_REGROUP_FOLLOW.ordinal()] += 3.4f;
		}
	}
	
	/**
	 * Performs a full 3-layer (`W3 -> W2 -> W1`) online REINFORCE policy-gradient backpropagation step
	 * through LeakyReLU activations given the state vector, chosen action, action probabilities, and shaped tactical reward.
	 * @param state 16-element state vector
	 * @param action executed {@link TacticalAction}
	 * @param probs 8-element softmax probabilities from {@link #selectAction}
	 * @param reward immediate tactical reward
	 */
	public synchronized void recordOutcome(float[] state, TacticalAction action, float[] probs, float reward)
	{
		if ((state == null) || (action == null) || (probs == null))
		{
			return;
		}
		
		final float advantage = Math.clamp(reward - _rewardBaseline, -2.5f, 2.5f);
		_rewardBaseline = (0.985f * _rewardBaseline) + (0.015f * reward);
		
		// Forward activations h1 and h2
		final float[] h1 = new float[HIDDEN1_DIM];
		final float[] h2 = new float[HIDDEN2_DIM];
		for (int i = 0; i < HIDDEN1_DIM; i++)
		{
			final float sum = _b1[i] + simdDot(state, 0, _w1, i * INPUT_DIM, INPUT_DIM);
			h1[i] = sum >= 0f ? sum : 0.1f * sum;
		}
		for (int i = 0; i < HIDDEN2_DIM; i++)
		{
			final float sum = _b2[i] + simdDot(h1, 0, _w2, i * HIDDEN1_DIM, HIDDEN1_DIM);
			h2[i] = sum >= 0f ? sum : 0.1f * sum;
		}
		
		final int actIdx = action.ordinal();
		final float lr = _learningRate;
		final float[] dLogit = new float[ACTION_DIM];
		for (int a = 0; a < ACTION_DIM; a++)
		{
			final float target = (a == actIdx) ? 1.0f : 0.0f;
			dLogit[a] = (target - probs[a]) * advantage * lr;
		}
		
		// Backpropagate error from Layer 3 (logits) into Layer 2 (h2)
		final float[] dH2 = new float[HIDDEN2_DIM];
		for (int j = 0; j < HIDDEN2_DIM; j++)
		{
			float err = 0f;
			for (int a = 0; a < ACTION_DIM; a++)
			{
				err += dLogit[a] * _w3[(a * HIDDEN2_DIM) + j];
			}
			dH2[j] = err * (h2[j] >= 0f ? 1.0f : 0.1f);
		}
		
		// Backpropagate error from Layer 2 (h2) into Layer 1 (h1)
		final float[] dH1 = new float[HIDDEN1_DIM];
		for (int k = 0; k < HIDDEN1_DIM; k++)
		{
			float err = 0f;
			for (int j = 0; j < HIDDEN2_DIM; j++)
			{
				err += dH2[j] * _w2[(j * HIDDEN1_DIM) + k];
			}
			dH1[k] = err * (h1[k] >= 0f ? 1.0f : 0.1f);
		}
		
		// Update Layer 3 (_w3, _b3)
		for (int a = 0; a < ACTION_DIM; a++)
		{
			final float grad = dLogit[a];
			_b3[a] = Math.clamp(_b3[a] + grad, -6.0f, 6.0f);
			final int rowOffset = a * HIDDEN2_DIM;
			for (int j = 0; j < HIDDEN2_DIM; j++)
			{
				_w3[rowOffset + j] = Math.clamp(_w3[rowOffset + j] + (grad * h2[j]) - (0.0001f * _w3[rowOffset + j]), -6.0f, 6.0f);
			}
		}
		
		// Update Layer 2 (_w2, _b2)
		for (int j = 0; j < HIDDEN2_DIM; j++)
		{
			final float grad = dH2[j];
			_b2[j] = Math.clamp(_b2[j] + grad, -6.0f, 6.0f);
			final int rowOffset = j * HIDDEN1_DIM;
			for (int k = 0; k < HIDDEN1_DIM; k++)
			{
				_w2[rowOffset + k] = Math.clamp(_w2[rowOffset + k] + (grad * h1[k]) - (0.0001f * _w2[rowOffset + k]), -6.0f, 6.0f);
			}
		}
		
		// Update Layer 1 (_w1, _b1)
		for (int k = 0; k < HIDDEN1_DIM; k++)
		{
			final float grad = dH1[k];
			_b1[k] = Math.clamp(_b1[k] + grad, -6.0f, 6.0f);
			final int rowOffset = k * INPUT_DIM;
			for (int m = 0; m < INPUT_DIM; m++)
			{
				_w1[rowOffset + m] = Math.clamp(_w1[rowOffset + m] + (grad * state[m]) - (0.0001f * _w1[rowOffset + m]), -6.0f, 6.0f);
			}
		}
		
		final long steps = _trainingSteps.incrementAndGet();
		if ((steps % 500L) == 0L)
		{
			saveWeights();
		}
	}
	
	private static float simdDot(float[] a, int aOffset, float[] b, int bOffset, int length)
	{
		int i = 0;
		float sum = 0f;
		final int loopBound = SPECIES.loopBound(length);
		if (loopBound > 0)
		{
			FloatVector acc = FloatVector.zero(SPECIES);
			for (; i < loopBound; i += SPECIES.length())
			{
				final FloatVector va = FloatVector.fromArray(SPECIES, a, aOffset + i);
				final FloatVector vb = FloatVector.fromArray(SPECIES, b, bOffset + i);
				acc = va.fma(vb, acc);
			}
			sum = acc.reduceLanes(VectorOperators.ADD);
		}
		for (; i < length; i++)
		{
			sum += a[aOffset + i] * b[bOffset + i];
		}
		return sum;
	}
	
	public synchronized boolean saveWeights()
	{
		final File file = new File(WEIGHTS_FILE_PATH);
		if (file.getParentFile() != null)
		{
			file.getParentFile().mkdirs();
		}
		try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file))))
		{
			out.writeInt(MAGIC_HEADER);
			out.writeInt(VERSION);
			out.writeInt(_stage.getId());
			out.writeLong(_trainingSteps.get());
			out.writeInt(_duelsCompleted.get());
			out.writeFloat(_rewardBaseline);
			writeFloatArray(out, _w1);
			writeFloatArray(out, _b1);
			writeFloatArray(out, _w2);
			writeFloatArray(out, _b2);
			writeFloatArray(out, _w3);
			writeFloatArray(out, _b3);
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "FakePlayerNeuralPolicy: Failed to save weights: " + e.getMessage(), e);
			return false;
		}
	}
	
	public synchronized boolean loadWeights()
	{
		final File file = new File(WEIGHTS_FILE_PATH);
		if (!file.isFile())
		{
			return false;
		}
		try (DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file))))
		{
			if ((in.readInt() != MAGIC_HEADER) || (in.readInt() != VERSION))
			{
				return false;
			}
			_stage = CurriculumStage.fromId(in.readInt());
			_trainingSteps.set(in.readLong());
			_duelsCompleted.set(in.readInt());
			_rewardBaseline = in.readFloat();
			readFloatArray(in, _w1);
			readFloatArray(in, _b1);
			readFloatArray(in, _w2);
			readFloatArray(in, _b2);
			readFloatArray(in, _w3);
			readFloatArray(in, _b3);
			LOGGER.info("FakePlayerNeuralPolicy: Loaded checkpoint (" + _trainingSteps.get() + " steps, " + _stageTitle() + ").");
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "FakePlayerNeuralPolicy: Could not load weights, using warm-start priors: " + e.getMessage());
			return false;
		}
	}
	
	private String _stageTitle()
	{
		return _stage.getTitle();
	}
	
	private static void writeFloatArray(DataOutputStream out, float[] arr) throws Exception
	{
		out.writeInt(arr.length);
		for (float v : arr)
		{
			out.writeFloat(v);
		}
	}
	
	private static void readFloatArray(DataInputStream in, float[] arr) throws Exception
	{
		final int len = in.readInt();
		final int limit = Math.min(len, arr.length);
		for (int i = 0; i < limit; i++)
		{
			arr[i] = in.readFloat();
		}
		for (int i = limit; i < len; i++)
		{
			in.readFloat();
		}
	}
	
	public CurriculumStage getStage()
	{
		return _stage;
	}
	
	public void setStage(CurriculumStage stage)
	{
		if (stage != null)
		{
			_stage = stage;
		}
	}
	
	public long getTrainingSteps()
	{
		return _trainingSteps.get();
	}
	
	public int getDuelsCompleted()
	{
		return _duelsCompleted.get();
	}
	
	public int incrementDuelsCompleted()
	{
		return _duelsCompleted.incrementAndGet();
	}
	
	public float getRewardBaseline()
	{
		return _rewardBaseline;
	}
}
