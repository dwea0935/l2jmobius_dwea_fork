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

import quests.Q00127_FishingSpecialistsRequest.Q00127_FishingSpecialistsRequest;
import quests.Q00255_Tutorial.Q00255_Tutorial;
import quests.Q00502_BrothersBoundInChains.Q00502_BrothersBoundInChains;
import quests.Q00662_AGameOfCards.Q00662_AGameOfCards;
import quests.Q10290_ATripBegins.Q10290_ATripBegins;
import quests.Q10291_MoreExperience.Q10291_MoreExperience;
import quests.Q10292_SecretGarden.Q10292_SecretGarden;
import quests.Q10293_DeathMysteries.Q10293_DeathMysteries;
import quests.Q10294_SporeInfestedPlace.Q10294_SporeInfestedPlace;
import quests.Q10295_RespectForGraves.Q10295_RespectForGraves;
import quests.Q10296_LetsPayRespectsToOurFallenBrethren.Q10296_LetsPayRespectsToOurFallenBrethren;
import quests.Q10297_MemoryOfTheGloriousPast.Q10297_MemoryOfTheGloriousPast;
import quests.Q10298_TracesOfBattle.Q10298_TracesOfBattle;
import quests.Q10299_GetIncrediblePower.Q10299_GetIncrediblePower;
import quests.Q10300_ExploringTheCrumaTower.Q10300_ExploringTheCrumaTower;
import quests.Q10301_NotSoSilentValley.Q10301_NotSoSilentValley;
import quests.Q10302_FoilPlansOfTheLizardmen.Q10302_FoilPlansOfTheLizardmen;
import quests.Q10303_SymbolOfHubris.Q10303_SymbolOfHubris;
import quests.Q10304_ChangesintheDragonValley.Q10304_ChangesintheDragonValley;
import quests.Q10305_DragonsSuspiciousMovements.Q10305_DragonsSuspiciousMovements;
import quests.Q10306_StopSelMahumsTroops.Q10306_StopSelMahumsTroops;
import quests.Q10307_TurekOrcsSecret.Q10307_TurekOrcsSecret;
import quests.Q10673_SagaOfLegend.Q10673_SagaOfLegend;
import quests.Q10954_SayhaChildren.Q10954_SayhaChildren;
import quests.Q10955_NewLifeLessons.Q10955_NewLifeLessons;
import quests.Q10956_WeSylphs.Q10956_WeSylphs;
import quests.Q10957_TheLifeOfADeathKnight.Q10957_TheLifeOfADeathKnight;
import quests.Q10958_ExploringNewOpportunities.Q10958_ExploringNewOpportunities;
import quests.Q10959_ChallengingYourDestiny.Q10959_ChallengingYourDestiny;
import quests.Q10961_EffectiveTraining.Q10961_EffectiveTraining;
import quests.Q10962_NewHorizons.Q10962_NewHorizons;
import quests.Q10968_ThePowerOfTheMagicLamp.Q10968_ThePowerOfTheMagicLamp;
import quests.Q10971_TalismanEnchant.Q10971_TalismanEnchant;
import quests.Q10972_CombiningGems.Q10972_CombiningGems;
import quests.Q10973_EnchantingAgathions.Q10973_EnchantingAgathions;
import quests.Q10974_NewStylishEquipment.Q10974_NewStylishEquipment;
import quests.Q10978_MissingPets.Q10978_MissingPets;
import quests.Q10981_UnbearableWolvesHowling.Q10981_UnbearableWolvesHowling;
import quests.Q10982_SpiderHunt.Q10982_SpiderHunt;
import quests.Q10983_TroubledForest.Q10983_TroubledForest;
import quests.Q10984_CollectSpiderweb.Q10984_CollectSpiderweb;
import quests.Q10985_CleaningUpTheGround.Q10985_CleaningUpTheGround;
import quests.Q10986_SwampMonster.Q10986_SwampMonster;
import quests.Q10987_PlunderedGraves.Q10987_PlunderedGraves;
import quests.Q10988_Conspiracy.Q10988_Conspiracy;
import quests.Q10989_DangerousPredators.Q10989_DangerousPredators;
import quests.Q10990_PoisonExtraction.Q10990_PoisonExtraction;

/**
 * @author NosBit, Mobius
 */
public class QuestMasterHandler
{
	private static final Logger LOGGER = Logger.getLogger(QuestMasterHandler.class.getName());
	
	private static final Class<?>[] QUESTS =
	{
		Q00127_FishingSpecialistsRequest.class,
		Q00255_Tutorial.class,
		Q00502_BrothersBoundInChains.class,
		Q00662_AGameOfCards.class,
		Q10673_SagaOfLegend.class,
		Q10290_ATripBegins.class,
		Q10291_MoreExperience.class,
		Q10292_SecretGarden.class,
		Q10293_DeathMysteries.class,
		Q10294_SporeInfestedPlace.class,
		Q10295_RespectForGraves.class,
		Q10296_LetsPayRespectsToOurFallenBrethren.class,
		Q10297_MemoryOfTheGloriousPast.class,
		Q10298_TracesOfBattle.class,
		Q10299_GetIncrediblePower.class,
		Q10300_ExploringTheCrumaTower.class,
		Q10301_NotSoSilentValley.class,
		Q10302_FoilPlansOfTheLizardmen.class,
		Q10303_SymbolOfHubris.class,
		Q10304_ChangesintheDragonValley.class,
		Q10305_DragonsSuspiciousMovements.class,
		Q10306_StopSelMahumsTroops.class,
		Q10307_TurekOrcsSecret.class,
		Q10954_SayhaChildren.class,
		Q10955_NewLifeLessons.class,
		Q10956_WeSylphs.class,
		Q10957_TheLifeOfADeathKnight.class,
		Q10958_ExploringNewOpportunities.class,
		Q10959_ChallengingYourDestiny.class,
		Q10961_EffectiveTraining.class,
		Q10962_NewHorizons.class,
		Q10981_UnbearableWolvesHowling.class,
		Q10982_SpiderHunt.class,
		Q10983_TroubledForest.class,
		Q10984_CollectSpiderweb.class,
		Q10985_CleaningUpTheGround.class,
		Q10986_SwampMonster.class,
		Q10987_PlunderedGraves.class,
		Q10988_Conspiracy.class,
		Q10989_DangerousPredators.class,
		Q10990_PoisonExtraction.class,
		Q10968_ThePowerOfTheMagicLamp.class,
		Q10971_TalismanEnchant.class,
		Q10972_CombiningGems.class,
		Q10973_EnchantingAgathions.class,
		Q10974_NewStylishEquipment.class,
		Q10978_MissingPets.class,
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
