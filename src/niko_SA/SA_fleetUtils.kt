package niko_SA

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.RepLevel
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_miscUtils.isStationFleet

object SA_fleetUtils {
    @JvmStatic
    fun BattleAPI.getStationFleet(): CampaignFleetAPI? {
        val stationFleets = getStationFleets()
        return stationFleets.firstOrNull()
    }

    @JvmStatic
    private fun BattleAPI.getStationFleets(): List<CampaignFleetAPI> {
        val stationFleets = ArrayList<CampaignFleetAPI>()
        for (potentialStationFleet in stationSide) {
            if (potentialStationFleet.isStationFleet()) {
                stationFleets += potentialStationFleet
            }
        }
        if (stationFleets.size > 1) {
            SA_debugUtils.log.error("found more than 1 station fleet during getStationFleet")
        }
        return stationFleets
    }

    @JvmStatic
    fun CampaignFleetAPI.isStationFleet(): Boolean {
        return (isStationMode || memoryWithoutUpdate.contains(MemFlags.STATION_MARKET))
    }

    val defaultFriendliesForArrayBonus = hashMapOf(
        Pair(FleetTypes.TRADE, RepLevel.INHOSPITABLE),
        Pair(FleetTypes.TRADE_SMUGGLER, RepLevel.INHOSPITABLE),
        Pair(FleetTypes.TRADE_SMALL, RepLevel.INHOSPITABLE),
        Pair(FleetTypes.TRADE_LINER, RepLevel.INHOSPITABLE),
        Pair(FleetTypes.FOOD_RELIEF_FLEET, RepLevel.INHOSPITABLE),
        Pair(FleetTypes.SHRINE_PILGRIMS, RepLevel.INHOSPITABLE),
        Pair(FleetTypes.ACADEMY_FLEET, RepLevel.INHOSPITABLE),
    )
    fun CampaignFleetAPI.getRepLevelForArrayBonus(
        repMap: MutableMap<String, RepLevel> = defaultFriendliesForArrayBonus,
        defaultRep: RepLevel = RepLevel.FRIENDLY,
    ): RepLevel {

        val fleetType: String = memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_TYPE] as? String ?: return defaultRep
        return repMap[fleetType] ?: return defaultRep
    }
}