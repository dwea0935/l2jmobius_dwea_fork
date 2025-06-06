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
package org.l2jmobius.gameserver.network;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import org.l2jmobius.Config;
import org.l2jmobius.gameserver.network.clientpackets.*;

/**
 * @author Mobius
 */
public enum ExClientPackets
{
	REQUEST_GOTO_LOBBY(0x36, RequestGotoLobby::new, ConnectionState.AUTHENTICATED),
	REQUEST_MANOR_LIST(0x01, RequestManorList::new, ConnectionState.IN_GAME),
	REQUEST_PROCEDURE_CROP_LIST(0x02, RequestProcureCropList::new, ConnectionState.IN_GAME),
	REQUEST_SET_SEED(0x03, RequestSetSeed::new, ConnectionState.IN_GAME),
	REQUEST_SET_CROP(0x04, RequestSetCrop::new, ConnectionState.IN_GAME),
	REQUEST_WRITE_HERO_WORDS(0x05, RequestWriteHeroWords::new, ConnectionState.IN_GAME),
	REQUEST_EX_ASK_JOIN_MPCC(0x06, RequestExAskJoinMPCC::new, ConnectionState.IN_GAME),
	REQUEST_EX_ACCEPT_JOIN_MPCC(0x07, RequestExAcceptJoinMPCC::new, ConnectionState.IN_GAME),
	REQUEST_EX_OUST_FROM_MPCC(0x08, RequestExOustFromMPCC::new, ConnectionState.IN_GAME),
	REQUEST_OUST_FROM_PARTY_ROOM(0x09, RequestOustFromPartyRoom::new, ConnectionState.IN_GAME),
	REQUEST_DISMISS_PARTY_ROOM(0x0A, RequestDismissPartyRoom::new, ConnectionState.IN_GAME),
	REQUEST_WITHDRAW_PARTY_ROOM(0x0B, RequestWithdrawPartyRoom::new, ConnectionState.IN_GAME),
	REQUEST_CHANGE_PARTY_LEADER(0x0C, RequestChangePartyLeader::new, ConnectionState.IN_GAME),
	REQUEST_AUTO_SOULSHOT(0x0D, RequestAutoSoulShot::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_SKILL_INFO(0x0E, RequestExEnchantSkillInfo::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_SKILL(0x0F, RequestExEnchantSkill::new, ConnectionState.IN_GAME),
	REQUEST_EX_PLEDGE_CREST_LARGE(0x10, RequestExPledgeCrestLarge::new, ConnectionState.IN_GAME),
	REQUEST_EX_SET_PLEDGE_CREST_LARGE(0x11, RequestExSetPledgeCrestLarge::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_SET_ACADEMY_MASTER(0x12, RequestPledgeSetAcademyMaster::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_POWER_GRADE_LIST(0x13, RequestPledgePowerGradeList::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_MEMBER_POWER_INFO(0x14, RequestPledgeMemberPowerInfo::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_SET_MEMBER_POWER_GRADE(0x15, RequestPledgeSetMemberPowerGrade::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_MEMBER_INFO(0x16, RequestPledgeMemberInfo::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_WAR_LIST(0x17, RequestPledgeWarList::new, ConnectionState.IN_GAME),
	REQUEST_EX_FISH_RANKING(0x18, RequestExFishRanking::new, ConnectionState.IN_GAME),
	REQUEST_PCCAFE_COUPON_USE(0x19, RequestPCCafeCouponUse::new, ConnectionState.IN_GAME),
	REQUEST_SERVER_LOGIN(0x1A, null, ConnectionState.IN_GAME),
	REQUEST_DUEL_START(0x1B, RequestDuelStart::new, ConnectionState.IN_GAME),
	REQUEST_DUAL_ANSWER_START(0x1C, RequestDuelAnswerStart::new, ConnectionState.IN_GAME),
	REQUEST_EX_SET_TUTORIAL(0x1D, null, ConnectionState.IN_GAME),
	REQUEST_EX_RQ_ITEM_LINK(0x1E, RequestExRqItemLink::new, ConnectionState.IN_GAME),
	CANNOT_MOVE_ANYMORE_AIR_SHIP(0x1F, null, ConnectionState.IN_GAME),
	MOVE_TO_LOCATION_IN_AIR_SHIP(0x20, MoveToLocationInAirShip::new, ConnectionState.IN_GAME),
	REQUEST_KEY_MAPPING(0x21, RequestKeyMapping::new, ConnectionState.ENTERING, ConnectionState.IN_GAME),
	REQUEST_SAVE_KEY_MAPPING(0x22, RequestSaveKeyMapping::new, ConnectionState.IN_GAME),
	REQUEST_EX_REMOVE_ITEM_ATTRIBUTE(0x23, RequestExRemoveItemAttribute::new, ConnectionState.IN_GAME),
	REQUEST_SAVE_INVENTORY_ORDER(0x24, RequestSaveInventoryOrder::new, ConnectionState.IN_GAME),
	REQUEST_EXIT_PARTY_MATCHING_WAITING_ROOM(0x25, RequestExitPartyMatchingWaitingRoom::new, ConnectionState.IN_GAME),
	REQUEST_CONFIRM_TARGET_ITEM(0x26, RequestConfirmTargetItem::new, ConnectionState.IN_GAME),
	REQUEST_CONFIRM_REFINER_ITEM(0x27, RequestConfirmRefinerItem::new, ConnectionState.IN_GAME),
	REQUEST_CONFIRM_GEMSTONE(0x28, RequestConfirmGemStone::new, ConnectionState.IN_GAME),
	REQUEST_OLYMPIAD_OBSERVER_END(0x29, RequestOlympiadObserverEnd::new, ConnectionState.IN_GAME),
	REQUEST_CURSED_WEAPON_LIST(0x2A, RequestCursedWeaponList::new, ConnectionState.IN_GAME),
	REQUEST_CURSED_WEAPON_LOCATION(0x2B, RequestCursedWeaponLocation::new, ConnectionState.IN_GAME),
	REQUEST_PLEDGE_REORGANIZE_MEMBER(0x2C, RequestPledgeReorganizeMember::new, ConnectionState.IN_GAME),
	REQUEST_EX_MPCC_SHOW_PARTY_MEMBERS_INFO(0x2D, RequestExMPCCShowPartyMembersInfo::new, ConnectionState.IN_GAME),
	REQUEST_OLYMPIAD_MATCH_LIST(0x2E, RequestOlympiadMatchList::new, ConnectionState.IN_GAME),
	REQUEST_ASK_JOIN_PARTY_ROOM(0x2F, RequestAskJoinPartyRoom::new, ConnectionState.IN_GAME),
	ANSWER_JOIN_PARTY_ROOM(0x30, AnswerJoinPartyRoom::new, ConnectionState.IN_GAME),
	REQUEST_LIST_PARTY_MATCHING_WAITING_ROOM(0x31, RequestListPartyMatchingWaitingRoom::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_SKILL_SAFE(0x32, RequestExEnchantSkillSafe::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_SKILL_UNTRAIN(0x33, RequestExEnchantSkillUntrain::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_SKILL_ROUTE_CHANGE(0x34, RequestExEnchantSkillRouteChange::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_ITEM_ATTRIBUTE(0x35, RequestExEnchantItemAttribute::new, ConnectionState.IN_GAME),
	MOVE_TO_LOCATION_AIR_SHIP(0x38, MoveToLocationAirShip::new, ConnectionState.IN_GAME),
	REQUEST_BID_ITEM_AUCTION(0x39, RequestBidItemAuction::new, ConnectionState.IN_GAME),
	REQUEST_INFO_ITEM_AUCTION(0x3A, RequestInfoItemAuction::new, ConnectionState.IN_GAME),
	REQUEST_EX_CHANGE_NAME(0x3B, RequestExChangeName::new, ConnectionState.IN_GAME),
	REQUEST_ALL_CASTLE_INFO(0x3C, RequestAllCastleInfo::new, ConnectionState.IN_GAME),
	REQUEST_ALL_FORTRESS_INFO(0x3D, RequestAllFortressInfo::new, ConnectionState.IN_GAME),
	REQUEST_ALL_AGIT_INGO(0x3e, RequestAllAgitInfo::new, ConnectionState.IN_GAME),
	REQUEST_FORTRESS_SIEGE_INFO(0x3F, RequestFortressSiegeInfo::new, ConnectionState.IN_GAME),
	REQUEST_GET_BOSS_RECORD(0x40, RequestGetBossRecord::new, ConnectionState.IN_GAME),
	REQUEST_REFINE(0x41, RequestRefine::new, ConnectionState.IN_GAME),
	REQUEST_CONFIRM_CANCEL_ITEM(0x42, RequestConfirmCancelItem::new, ConnectionState.IN_GAME),
	REQUEST_REFINE_CANCEL(0x43, RequestRefineCancel::new, ConnectionState.IN_GAME),
	REQUEST_EX_MAGIC_SKILL_USE_GROUND(0x44, RequestExMagicSkillUseGround::new, ConnectionState.IN_GAME),
	REQUEST_DUEL_SURRENDER(0x45, RequestDuelSurrender::new, ConnectionState.IN_GAME),
	REQUEST_EX_ENCHANT_SKILL_INFO_DETAIL(0x46, RequestExEnchantSkillInfoDetail::new, ConnectionState.IN_GAME),
	REQUEST_FORTRESS_MAP_INFO(0x48, RequestFortressMapInfo::new, ConnectionState.IN_GAME),
	SET_PRIVATE_STORE_WHOLE_MSG(0x49, SetPrivateStoreWholeMsg::new, ConnectionState.IN_GAME),
	REQUEST_DISPEL(0x4B, RequestDispel::new, ConnectionState.IN_GAME),
	REQUEST_EX_TRY_TO_PUT_ENCHANT_TARGET_ITEM(0x4C, RequestExTryToPutEnchantTargetItem::new, ConnectionState.IN_GAME),
	REQUEST_EX_TRY_TO_PUT_ENCHANT_SUPPORT_ITEM(0x4D, RequestExTryToPutEnchantSupportItem::new, ConnectionState.IN_GAME),
	REQUEST_EX_CANCEL_ENCHANT_ITEM(0x4E, RequestExCancelEnchantItem::new, ConnectionState.IN_GAME),
	REQUEST_CHANGE_NICKNAME_COLOR(0x4F, RequestChangeNicknameColor::new, ConnectionState.IN_GAME),
	REQUEST_RESET_NICKNAME(0x50, RequestResetNickname::new, ConnectionState.IN_GAME),
	EX_BOOKMARK_PACKET(0x51, null, ConnectionState.IN_GAME),
	REQUEST_WITHDRAW_PREMIUM_ITEM(0x52, RequestWithDrawPremiumItem::new, ConnectionState.IN_GAME),
	REQUEST_EX_JUMP(0x53, null, ConnectionState.IN_GAME),
	REQUEST_EX_START_SHOW_CRATAE_CUBE_RANK(0x54, null, ConnectionState.IN_GAME),
	REQUEST_EX_STOP_SHOW_CRATAE_CUBE_RANK(0x55, null, ConnectionState.IN_GAME),
	NOTIFY_START_MINI_GAME(0x56, null, ConnectionState.IN_GAME),
	REQUEST_EX_JOIN_DOMINION_WAR(0x57, RequestJoinDominionWar::new, ConnectionState.IN_GAME),
	REQUEST_EX_DOMINION_INFO(0x58, RequestDominionInfo::new, ConnectionState.IN_GAME),
	REQUEST_EX_CLEFT_ENTER(0x59, null, ConnectionState.IN_GAME),
	REQUEST_EX_CUBE_GAME_CHANGE_TEAM(0x5A, RequestExCubeGameChangeTeam::new, ConnectionState.IN_GAME),
	END_SCENE_PLAYER(0x5B, EndScenePlayer::new, ConnectionState.IN_GAME),
	REQUEST_EX_CUBE_GAME_READY_ANSWER(0x5C, RequestExCubeGameReadyAnswer::new, ConnectionState.IN_GAME),
	REQUEST_EX_LIST_MPCC_WAITING(0x5D, null, ConnectionState.IN_GAME),
	REQUEST_EX_MANAGE_MPCC_ROOM(0x5E, null, ConnectionState.IN_GAME),
	REQUEST_EX_JOIN_MPCC_ROOM(0x5F, null, ConnectionState.IN_GAME),
	REQUEST_EX_OUST_FROM_MPCC_ROOM(0x60, null, ConnectionState.IN_GAME),
	REQUEST_EX_DISMISS_MPCC_ROOM(0x61, null, ConnectionState.IN_GAME),
	REQUEST_EX_WITHDRAW_MPCC_ROOM(0x62, null, ConnectionState.IN_GAME),
	REQUEST_SEED_PHASE(0x63, RequestSeedPhase::new, ConnectionState.IN_GAME),
	REQUEST_EX_MPCC_PARTYMASTER_LIST(0x64, null, ConnectionState.IN_GAME),
	REQUEST_POST_ITEM_LIST(0x65, RequestPostItemList::new, ConnectionState.IN_GAME),
	REQUEST_SEND_POST(0x66, RequestSendPost::new, ConnectionState.IN_GAME),
	REQUEST_RECEIVED_POST_LIST(0x67, RequestReceivedPostList::new, ConnectionState.IN_GAME),
	REQUEST_DELETE_RECEIVED_POST(0x68, RequestDeleteReceivedPost::new, ConnectionState.IN_GAME),
	REQUEST_RECEIVED_POST(0x69, RequestReceivedPost::new, ConnectionState.IN_GAME),
	REQUEST_POST_ATTACHMENT(0x6A, RequestPostAttachment::new, ConnectionState.IN_GAME),
	REQUEST_REJECT_POST_ATTACHMENT(0x6B, RequestRejectPostAttachment::new, ConnectionState.IN_GAME),
	REQUEST_SENT_POST_LIST(0x6C, RequestSentPostList::new, ConnectionState.IN_GAME),
	REQUEST_DELETE_SENT_POST(0x6D, RequestDeleteSentPost::new, ConnectionState.IN_GAME),
	REQUEST_SENT_POST(0x6E, RequestSentPost::new, ConnectionState.IN_GAME),
	REQUEST_CANCEL_SENT_POST(0x6F, RequestCancelPostAttachment::new, ConnectionState.IN_GAME),
	REQUEST_SHOW_NEW_USER_PETITION(0x70, null, ConnectionState.IN_GAME),
	REQUEST_SHOW_STEP_TWO(0x71, null, ConnectionState.IN_GAME),
	REQUEST_SHOW_STEP_THREE(0x72, null, ConnectionState.IN_GAME),
	EX_CONNECT_TO_RAID_SERVER(0x73, null, ConnectionState.IN_GAME),
	EX_RETURN_FROM_RAID_SERVER(0x74, null, ConnectionState.IN_GAME),
	REQUEST_REFUND_ITEM(0x75, RequestRefundItem::new, ConnectionState.IN_GAME),
	REQUEST_BUI_SELL_UI_CLOSE(0x76, RequestBuySellUIClose::new, ConnectionState.IN_GAME),
	REQUEST_EX_EVENT_MATCH_OBSERVER_END(0x77, null, ConnectionState.IN_GAME),
	REQUEST_BR_GAME_POINT(0x78, RequestBrGamePoint::new, ConnectionState.IN_GAME),
	REQUEST_BR_PRODUCT_LIST(0x79, RequestBrProductList::new, ConnectionState.IN_GAME),
	REQUEST_BR_PRODUCT_INFO(0x7A, RequestBrProductInfo::new, ConnectionState.IN_GAME),
	REQUEST_BR_BUI_PRODUCT(0x7B, RequestBrBuyProduct::new, ConnectionState.IN_GAME),
	REQUEST_BR_RECENT_PRODUCT_LIST(0x7C, RequestBrRecentProductList::new, ConnectionState.IN_GAME),
	BR_EVENT_RANKER_LIST(0x7D, BrEventRankerList::new, ConnectionState.IN_GAME),
	REQUEST_BR_MINI_GAME_LOAD_SCORES(0x7E, null, ConnectionState.IN_GAME),
	REQUEST_BR_MINI_GAME_INSERT_SCORE(0x7F, null, ConnectionState.IN_GAME),
	REQUEST_BR_LECTURE_MARK(0x80, null, ConnectionState.IN_GAME);
	
	public static final ExClientPackets[] PACKET_ARRAY;
	static
	{
		final int maxPacketId = Arrays.stream(values()).mapToInt(ExClientPackets::getPacketId).max().orElse(0);
		PACKET_ARRAY = new ExClientPackets[maxPacketId + 1];
		for (ExClientPackets packet : values())
		{
			PACKET_ARRAY[packet.getPacketId()] = packet;
		}
	}
	
	private final int _packetId;
	private final Supplier<ClientPacket> _packetSupplier;
	private final Set<ConnectionState> _connectionStates;
	
	ExClientPackets(int packetId, Supplier<ClientPacket> packetSupplier, ConnectionState... connectionStates)
	{
		// Packet id is an unsigned short.
		if (packetId > 0xFFFF)
		{
			throw new IllegalArgumentException("Packet id must not be bigger than 0xFFFF");
		}
		
		_packetId = packetId;
		_packetSupplier = packetSupplier != null ? packetSupplier : () -> null;
		_connectionStates = new HashSet<>(Arrays.asList(connectionStates));
	}
	
	public int getPacketId()
	{
		return _packetId;
	}
	
	public ClientPacket newPacket()
	{
		final ClientPacket packet = _packetSupplier.get();
		if (Config.DEBUG_EX_CLIENT_PACKETS)
		{
			if (packet != null)
			{
				final String name = packet.getClass().getSimpleName();
				if (!Config.ALT_DEV_EXCLUDED_PACKETS.contains(name))
				{
					PacketLogger.info("[C EX] " + name);
				}
			}
			else if (Config.DEBUG_UNKNOWN_PACKETS)
			{
				PacketLogger.info("[C EX] 0x" + Integer.toHexString(_packetId).toUpperCase());
			}
		}
		return packet;
	}
	
	public Set<ConnectionState> getConnectionStates()
	{
		return _connectionStates;
	}
}
