package niko_SA.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import niko_SA.SA_ids

/** Workaround to a vanilla "feature" where station memory is cleared if you leave a fleet engagement with them */
class stationMarketTracker: FleetEventListener {
    companion object {
        fun getInstance(): stationMarketTracker {
            var tracker: stationMarketTracker? = Global.getSector().memoryWithoutUpdate[SA_ids.SA_statonMarketTrackerMemId] as stationMarketTracker?
            if (tracker == null) {
                Global.getSector().memoryWithoutUpdate[SA_ids.SA_statonMarketTrackerMemId] = stationMarketTracker()
                tracker = Global.getSector().memoryWithoutUpdate[SA_ids.SA_statonMarketTrackerMemId] as stationMarketTracker?
            }
            return tracker!!
        }
    }

    private val stationsToMarket = HashMap<CampaignFleetAPI, MarketAPI>()

 /*   fun validate() { // not needed - see reportFleetDespawnedToListener
        for (entry in (HashMap(stationsToMarket).entries)) {
            val key = entry.key
            val value = entry.value

            if (!key.isAlive) {
                stationsToMarket.remove(key)
                continue
            }
        }
    }*/

    fun getMarketOfFleet(fleet: CampaignFleetAPI): MarketAPI? {
        //validate()

        val memMarket: MarketAPI? = fleet.memoryWithoutUpdate[MemFlags.STATION_MARKET] as MarketAPI?
        if (memMarket != null) {
            stationsToMarket[fleet] = memMarket // cache
            fleet.addEventListener(this)
            return memMarket
        }
        return stationsToMarket[fleet]
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {

        if (fleet == null) return

        stationsToMarket -= fleet
        fleet.removeEventListener(this)
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }
}