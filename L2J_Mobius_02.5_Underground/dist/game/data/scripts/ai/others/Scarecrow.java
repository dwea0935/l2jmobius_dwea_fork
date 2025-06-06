/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ai.others;

import org.l2jmobius.gameserver.model.actor.Npc;

import ai.AbstractNpcAI;

/**
 * Scarecrow AI.
 * @author ivantotov, Mobius
 */
public class Scarecrow extends AbstractNpcAI
{
	// NPCs
	private static final int TRAINING_DUMMY = 19546;
	private static final int SCARECROW = 27457;
	private static final int PHYSICAL_PUNCH_MACHINE = 33752;
	private static final int MAGICAL_PUNCH_MACHINE = 33753;
	
	private Scarecrow()
	{
		addSpawnId(TRAINING_DUMMY, SCARECROW, PHYSICAL_PUNCH_MACHINE, MAGICAL_PUNCH_MACHINE);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		npc.disableCoreAI(true);
		npc.setImmobilized(true);
	}
	
	public static void main(String[] args)
	{
		new Scarecrow();
	}
}
