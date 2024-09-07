package niko_SA

import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
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
}