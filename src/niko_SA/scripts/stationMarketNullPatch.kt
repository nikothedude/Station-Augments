/*package niko_SA.scripts // doesnt work, issue is with internal rules

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.MemFlags

/** Patches a bug where, apon exiting a combat with a station, its STATION_MARKET memflag is nulled */
class stationMarketNullPatch(
    val stationFleet: CampaignFleetAPI,
    val market: MarketAPI
): EveryFrameScript {
    var done = false

    override fun isDone(): Boolean = done
    override fun runWhilePaused(): Boolean = true

    override fun advance(amount: Float) {
        done = true
        val stationMember = stationFleet.fleetData?.membersListCopy?.firstOrNull()
        if (stationFleet.isEmpty || (stationMember != null && stationMember.status.hullFraction <= 0)) {
            return
        }
        stationFleet.memoryWithoutUpdate[MemFlags.STATION_MARKET] = market
    }
}*/