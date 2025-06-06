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
package quests;

import java.util.logging.Level;
import java.util.logging.Logger;

import quests.Q00500_BrothersBoundInChains.Q00500_BrothersBoundInChains;
import quests.Q00511_AwlUnderFoot.Q00511_AwlUnderFoot;
import quests.Q00512_BladeUnderFoot.Q00512_BladeUnderFoot;
import quests.Q00726_LightWithinTheDarkness.Q00726_LightWithinTheDarkness;
import quests.Q00727_HopeWithinTheDarkness.Q00727_HopeWithinTheDarkness;
import quests.Q00833_DevilsTreasureTauti.Q00833_DevilsTreasureTauti;
import quests.Q00835_PitiableMelisa.Q00835_PitiableMelisa;
import quests.Q00933_TombRaiders.Q00933_TombRaiders;
import quests.Q00937_ToReviveTheFishingGuild.Q00937_ToReviveTheFishingGuild;
import quests.Q00938_TheFishermansOtherHobby.Q00938_TheFishermansOtherHobby;
import quests.Q10423_EmbryoStrongholdRaid.Q10423_EmbryoStrongholdRaid;
import quests.Q10454_FinalEmbryoApostle.Q10454_FinalEmbryoApostle;
import quests.Q10507_ObtainingNewPower.Q10507_ObtainingNewPower;
import quests.Q10566_BestChoice.Q10566_BestChoice;
import quests.Q10575_LetsGoFishing.Q10575_LetsGoFishing;
import quests.Q10576_GlitteringWeapons.Q10576_GlitteringWeapons;
import quests.Q10577_TemperARustingBlade.Q10577_TemperARustingBlade;
import quests.Q10578_TheSoulOfASword.Q10578_TheSoulOfASword;
import quests.Q10579_ContainingTheAttributePower.Q10579_ContainingTheAttributePower;
import quests.Q10589_WhereFatesIntersect.Q10589_WhereFatesIntersect;
import quests.Q10590_ReawakenedFate.Q10590_ReawakenedFate;
import quests.Q10591_NobleMaterial.Q10591_NobleMaterial;
import quests.Q10801_TheDimensionalWarpPart1.Q10801_TheDimensionalWarpPart1;
import quests.Q10802_TheDimensionalWarpPart2.Q10802_TheDimensionalWarpPart2;
import quests.Q10803_TheDimensionalWarpPart3.Q10803_TheDimensionalWarpPart3;
import quests.Q10804_TheDimensionalWarpPart4.Q10804_TheDimensionalWarpPart4;
import quests.Q10805_TheDimensionalWarpPart5.Q10805_TheDimensionalWarpPart5;
import quests.Q10806_TheDimensionalWarpPart6.Q10806_TheDimensionalWarpPart6;
import quests.Q10807_TheDimensionalWarpPart7.Q10807_TheDimensionalWarpPart7;
import quests.Q10811_ExaltedOneWhoFacesTheLimit.Q10811_ExaltedOneWhoFacesTheLimit;
import quests.Q10817_ExaltedOneWhoOvercomesTheLimit.Q10817_ExaltedOneWhoOvercomesTheLimit;
import quests.Q10823_ExaltedOneWhoShattersTheLimit.Q10823_ExaltedOneWhoShattersTheLimit;
import quests.Q10873_ExaltedReachingAnotherLevel.Q10873_ExaltedReachingAnotherLevel;
import quests.Q10879_ExaltedGuideToPower.Q10879_ExaltedGuideToPower;
import quests.Q10885_SaviorsPathDiscovery.Q10885_SaviorsPathDiscovery;
import quests.Q10886_SaviorsPathSearchTheRefinery.Q10886_SaviorsPathSearchTheRefinery;
import quests.Q10887_SaviorsPathDemonsAndAtelia.Q10887_SaviorsPathDemonsAndAtelia;
import quests.Q10888_SaviorsPathDefeatTheEmbryo.Q10888_SaviorsPathDefeatTheEmbryo;
import quests.Q10889_SaviorsPathFallenEmperorsThrone.Q10889_SaviorsPathFallenEmperorsThrone;
import quests.Q10890_SaviorsPathHallOfEtina.Q10890_SaviorsPathHallOfEtina;
import quests.Q11024_PathOfDestinyBeginning.Q11024_PathOfDestinyBeginning;
import quests.Q11025_PathOfDestinyProving.Q11025_PathOfDestinyProving;
import quests.Q11026_PathOfDestinyConviction.Q11026_PathOfDestinyConviction;
import quests.Q11027_PathOfDestinyOvercome.Q11027_PathOfDestinyOvercome;
import quests.not_done.Q00504_CompetitionForTheBanditStronghold;
import quests.not_done.Q00655_AGrandPlanForTamingWildBeasts;
import quests.not_done.Q00836_RequestFromTheBlackbirdClan;
import quests.not_done.Q00837_RequestFromTheGiantTrackers;
import quests.not_done.Q00838_RequestFromTheMotherTreeGuardians;
import quests.not_done.Q00839_RequestFromTheUnworldlyVisitors;
import quests.not_done.Q00840_RequestFromTheKingdomsRoyalGuard;
import quests.not_done.Q01900_StormIsleSecretSpot;
import quests.not_done.Q01901_StormIsleFurtiveDeal;
import quests.not_done.Q10595_TheDimensionalWarpPart8;
import quests.not_done.Q10596_TheDimensionalWarpPart9;

/**
 * @author NosBit, Mobius
 */
public class QuestMasterHandler
{
	private static final Logger LOGGER = Logger.getLogger(QuestMasterHandler.class.getName());
	
	private static final Class<?>[] QUESTS =
	{
		Q00500_BrothersBoundInChains.class,
		Q00504_CompetitionForTheBanditStronghold.class, // TODO: Not done.
		Q00511_AwlUnderFoot.class,
		Q00512_BladeUnderFoot.class,
		Q00655_AGrandPlanForTamingWildBeasts.class, // TODO: Not done.
		Q00726_LightWithinTheDarkness.class,
		Q00727_HopeWithinTheDarkness.class,
		Q00833_DevilsTreasureTauti.class,
		Q00835_PitiableMelisa.class,
		Q00836_RequestFromTheBlackbirdClan.class, // TODO: Not done.
		Q00837_RequestFromTheGiantTrackers.class, // TODO: Not done.
		Q00838_RequestFromTheMotherTreeGuardians.class, // TODO: Not done.
		Q00839_RequestFromTheUnworldlyVisitors.class, // TODO: Not done.
		Q00840_RequestFromTheKingdomsRoyalGuard.class, // TODO: Not done.
		Q00933_TombRaiders.class,
		Q00937_ToReviveTheFishingGuild.class,
		Q00938_TheFishermansOtherHobby.class,
		Q01900_StormIsleSecretSpot.class, // TODO: Not done.
		Q01901_StormIsleFurtiveDeal.class, // TODO: Not done.
		Q10423_EmbryoStrongholdRaid.class,
		Q10454_FinalEmbryoApostle.class,
		Q10507_ObtainingNewPower.class,
		Q10566_BestChoice.class,
		Q10575_LetsGoFishing.class,
		Q10576_GlitteringWeapons.class,
		Q10577_TemperARustingBlade.class,
		Q10578_TheSoulOfASword.class,
		Q10579_ContainingTheAttributePower.class,
		Q10589_WhereFatesIntersect.class,
		Q10590_ReawakenedFate.class,
		Q10591_NobleMaterial.class,
		Q10595_TheDimensionalWarpPart8.class, // TODO: Not done.
		Q10596_TheDimensionalWarpPart9.class, // TODO: Not done.
		Q10801_TheDimensionalWarpPart1.class,
		Q10802_TheDimensionalWarpPart2.class,
		Q10803_TheDimensionalWarpPart3.class,
		Q10804_TheDimensionalWarpPart4.class,
		Q10805_TheDimensionalWarpPart5.class,
		Q10806_TheDimensionalWarpPart6.class,
		Q10807_TheDimensionalWarpPart7.class,
		Q10811_ExaltedOneWhoFacesTheLimit.class,
		Q10817_ExaltedOneWhoOvercomesTheLimit.class,
		Q10823_ExaltedOneWhoShattersTheLimit.class,
		Q10873_ExaltedReachingAnotherLevel.class,
		Q10879_ExaltedGuideToPower.class,
		Q10885_SaviorsPathDiscovery.class,
		Q10886_SaviorsPathSearchTheRefinery.class,
		Q10887_SaviorsPathDemonsAndAtelia.class,
		Q10888_SaviorsPathDefeatTheEmbryo.class,
		Q10889_SaviorsPathFallenEmperorsThrone.class,
		Q10890_SaviorsPathHallOfEtina.class,
		Q11024_PathOfDestinyBeginning.class,
		Q11025_PathOfDestinyProving.class,
		Q11026_PathOfDestinyConviction.class,
		Q11027_PathOfDestinyOvercome.class,
	};
	
	public static void main(String[] args)
	{
		for (Class<?> quest : QUESTS)
		{
			try
			{
				quest.getDeclaredConstructor().newInstance();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, QuestMasterHandler.class.getSimpleName() + ": Failed loading " + quest.getSimpleName() + ":", e);
			}
		}
	}
}
