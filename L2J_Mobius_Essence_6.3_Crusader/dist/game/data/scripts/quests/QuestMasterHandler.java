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
import quests.Q00401_BuryInOblivion.Q00401_BuryInOblivion;
import quests.Q00402_ArmedWithBows.Q00402_ArmedWithBows;
import quests.Q00403_TraceOfDeath.Q00403_TraceOfDeath;
import quests.Q00404_AnythingButSkeletons.Q00404_AnythingButSkeletons;
import quests.Q00405_GuardingTheFieldsOfMassacre.Q00405_GuardingTheFieldsOfMassacre;
import quests.Q00406_ExploringTheFieldsOfMassacre.Q00406_ExploringTheFieldsOfMassacre;
import quests.Q00408_ScoutStrike.Q00408_ScoutStrike;
import quests.Q00409_PunishVanorSilenos.Q00409_PunishVanorSilenos;
import quests.Q00410_MainStrike.Q00410_MainStrike;
import quests.Q00411_LeadersAndTheirMinions.Q00411_LeadersAndTheirMinions;
import quests.Q00412_VanorSilenosGuards.Q00412_VanorSilenosGuards;
import quests.Q00413_ExploringThePlainsOfGlory.Q00413_ExploringThePlainsOfGlory;
import quests.Q00415_ThoseCalledHatar.Q00415_ThoseCalledHatar;
import quests.Q00416_DeathBringers.Q00416_DeathBringers;
import quests.Q00417_WeirdCreatures.Q00417_WeirdCreatures;
import quests.Q00418_OrcsAllAround.Q00418_OrcsAllAround;
import quests.Q00419_ThoseLookingForBattle.Q00419_ThoseLookingForBattle;
import quests.Q00420_ExploringTheWarTornPlains.Q00420_ExploringTheWarTornPlains;
import quests.Q00422_CrumaTowers3rdFloor.Q00422_CrumaTowers3rdFloor;
import quests.Q00423_CrumaTower2ndFloor.Q00423_CrumaTower2ndFloor;
import quests.Q00424_CrumaTowersMagicalCreatures.Q00424_CrumaTowersMagicalCreatures;
import quests.Q00425_GiantsGolems.Q00425_GiantsGolems;
import quests.Q00426_SimilarYetDifferent.Q00426_SimilarYetDifferent;
import quests.Q00427_CrumaTowersExploration.Q00427_CrumaTowersExploration;
import quests.Q00429_PoorExcuseForGiants.Q00429_PoorExcuseForGiants;
import quests.Q00430_GiantsCreations.Q00430_GiantsCreations;
import quests.Q00431_TracesOfAncientCreatures.Q00431_TracesOfAncientCreatures;
import quests.Q00432_TracesOfExperiments.Q00432_TracesOfExperiments;
import quests.Q00433_NeverForgotten.Q00433_NeverForgotten;
import quests.Q00434_ExploringTheSilentValley.Q00434_ExploringTheSilentValley;
import quests.Q00436_BewareOfTantaLizardmen.Q00436_BewareOfTantaLizardmen;
import quests.Q00437_DangerousLizardmen.Q00437_DangerousLizardmen;
import quests.Q00438_AttacksFromAllSides.Q00438_AttacksFromAllSides;
import quests.Q00439_GettingRidOfSummoners.Q00439_GettingRidOfSummoners;
import quests.Q00440_GuardsInDisguise.Q00440_GuardsInDisguise;
import quests.Q00441_ExploringThePlainsOfTheLizardmen.Q00441_ExploringThePlainsOfTheLizardmen;
import quests.Q00443_BornFromDragonBlood.Q00443_BornFromDragonBlood;
import quests.Q00444_CurseOfTheDragonValley.Q00444_CurseOfTheDragonValley;
import quests.Q00445_WhereDemonsWhisper.Q00445_WhereDemonsWhisper;
import quests.Q00446_NoRightToDie.Q00446_NoRightToDie;
import quests.Q00447_DragonsTraces.Q00447_DragonsTraces;
import quests.Q00448_ExploringTheDragonValleyWest.Q00448_ExploringTheDragonValleyWest;
import quests.Q00450_MastersOfTheDragonValley.Q00450_MastersOfTheDragonValley;
import quests.Q00451_RiseOfTheDragons.Q00451_RiseOfTheDragons;
import quests.Q00452_MightyFighters.Q00452_MightyFighters;
import quests.Q00453_DragonMages.Q00453_DragonMages;
import quests.Q00454_ValleyDrakes.Q00454_ValleyDrakes;
import quests.Q00455_ExploringTheDragonValleyEast.Q00455_ExploringTheDragonValleyEast;
import quests.Q00457_NecessaryPrecautions.Q00457_NecessaryPrecautions;
import quests.Q00458_BornToFight.Q00458_BornToFight;
import quests.Q00459_SelMahumHorde.Q00459_SelMahumHorde;
import quests.Q00460_MagicForSmartOnes.Q00460_MagicForSmartOnes;
import quests.Q00461_InTheBasesBarracks.Q00461_InTheBasesBarracks;
import quests.Q00462_ExploringTheSelMahumBase.Q00462_ExploringTheSelMahumBase;
import quests.Q00464_BarracksMainForce.Q00464_BarracksMainForce;
import quests.Q00465_DontComeCloser.Q00465_DontComeCloser;
import quests.Q00466_TurekOrcsTenacity.Q00466_TurekOrcsTenacity;
import quests.Q00467_ImposingOrcs.Q00467_ImposingOrcs;
import quests.Q00468_TurekOrcLeaders.Q00468_TurekOrcLeaders;
import quests.Q00469_ExploringTheTurekOrcBarracks.Q00469_ExploringTheTurekOrcBarracks;
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
import quests.Q10304_ChangesintheDragonValley.Q10304_ChangesintheDragonValley;
import quests.Q10305_DragonsSuspiciousMovements.Q10305_DragonsSuspiciousMovements;
import quests.Q10306_StopSelMahumsTroops.Q10306_StopSelMahumsTroops;
import quests.Q10307_TurekOrcsSecret.Q10307_TurekOrcsSecret;
import quests.Q10308_TrainingForTheFuture.Q10308_TrainingForTheFuture;
import quests.Q10309_DreamlandsMysteries.Q10309_DreamlandsMysteries;
import quests.Q10310_VictoryInBalokBattleground.Q10310_VictoryInBalokBattleground;
import quests.Q10311_BestMedicine.Q10311_BestMedicine;
import quests.Q10312_GordesLegend.Q10312_GordesLegend;
import quests.Q10313_CunningMorgos.Q10313_CunningMorgos;
import quests.Q10314_MorgosRetributionXilenos.Q10314_MorgosRetributionXilenos;
import quests.Q10673_SagaOfLegend.Q10673_SagaOfLegend;
import quests.Q10949_PatternsAndHiddenPower.Q10949_PatternsAndHiddenPower;
import quests.Q10950_FiercestFlame.Q10950_FiercestFlame;
import quests.Q10951_NewFlameOfOrcs.Q10951_NewFlameOfOrcs;
import quests.Q10952_ProtectAtAllCosts.Q10952_ProtectAtAllCosts;
import quests.Q10953_ValiantOrcs.Q10953_ValiantOrcs;
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
		Q00401_BuryInOblivion.class,
		Q00402_ArmedWithBows.class,
		Q00403_TraceOfDeath.class,
		Q00404_AnythingButSkeletons.class,
		Q00405_GuardingTheFieldsOfMassacre.class,
		Q00406_ExploringTheFieldsOfMassacre.class,
		Q00408_ScoutStrike.class,
		Q00409_PunishVanorSilenos.class,
		Q00410_MainStrike.class,
		Q00411_LeadersAndTheirMinions.class,
		Q00412_VanorSilenosGuards.class,
		Q00413_ExploringThePlainsOfGlory.class,
		Q00415_ThoseCalledHatar.class,
		Q00416_DeathBringers.class,
		Q00417_WeirdCreatures.class,
		Q00418_OrcsAllAround.class,
		Q00419_ThoseLookingForBattle.class,
		Q00420_ExploringTheWarTornPlains.class,
		Q00422_CrumaTowers3rdFloor.class,
		Q00423_CrumaTower2ndFloor.class,
		Q00424_CrumaTowersMagicalCreatures.class,
		Q00425_GiantsGolems.class,
		Q00426_SimilarYetDifferent.class,
		Q00427_CrumaTowersExploration.class,
		Q00429_PoorExcuseForGiants.class,
		Q00430_GiantsCreations.class,
		Q00431_TracesOfAncientCreatures.class,
		Q00432_TracesOfExperiments.class,
		Q00433_NeverForgotten.class,
		Q00434_ExploringTheSilentValley.class,
		Q00436_BewareOfTantaLizardmen.class,
		Q00437_DangerousLizardmen.class,
		Q00438_AttacksFromAllSides.class,
		Q00439_GettingRidOfSummoners.class,
		Q00440_GuardsInDisguise.class,
		Q00441_ExploringThePlainsOfTheLizardmen.class,
		Q00443_BornFromDragonBlood.class,
		Q00444_CurseOfTheDragonValley.class,
		Q00445_WhereDemonsWhisper.class,
		Q00446_NoRightToDie.class,
		Q00447_DragonsTraces.class,
		Q00448_ExploringTheDragonValleyWest.class,
		Q00450_MastersOfTheDragonValley.class,
		Q00451_RiseOfTheDragons.class,
		Q00452_MightyFighters.class,
		Q00453_DragonMages.class,
		Q00454_ValleyDrakes.class,
		Q00455_ExploringTheDragonValleyEast.class,
		Q00457_NecessaryPrecautions.class,
		Q00458_BornToFight.class,
		Q00459_SelMahumHorde.class,
		Q00460_MagicForSmartOnes.class,
		Q00461_InTheBasesBarracks.class,
		Q00462_ExploringTheSelMahumBase.class,
		Q00464_BarracksMainForce.class,
		Q00465_DontComeCloser.class,
		Q00466_TurekOrcsTenacity.class,
		Q00467_ImposingOrcs.class,
		Q00468_TurekOrcLeaders.class,
		Q00469_ExploringTheTurekOrcBarracks.class,
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
		Q10304_ChangesintheDragonValley.class,
		Q10305_DragonsSuspiciousMovements.class,
		Q10306_StopSelMahumsTroops.class,
		Q10307_TurekOrcsSecret.class,
		Q10308_TrainingForTheFuture.class,
		Q10309_DreamlandsMysteries.class,
		Q10310_VictoryInBalokBattleground.class,
		Q10311_BestMedicine.class,
		Q10312_GordesLegend.class,
		Q10313_CunningMorgos.class,
		Q10314_MorgosRetributionXilenos.class,
		Q10949_PatternsAndHiddenPower.class,
		Q10950_FiercestFlame.class,
		Q10951_NewFlameOfOrcs.class,
		Q10952_ProtectAtAllCosts.class,
		Q10953_ValiantOrcs.class,
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
