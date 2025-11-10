package niko_SA.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignEventListener
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.listeners.FleetEventListener
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import niko_SA.SA_ids
import org.magiclib.kotlin.getStationFleet

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
    /// If we've failed searching once, we will never search again.
    private var dontSearchForMarket = HashSet<CampaignFleetAPI>()
        get() {
            if (field == null) field = HashSet<CampaignFleetAPI>()
            return field
        }

    fun findMarket(fleet: CampaignFleetAPI): MarketAPI? {
        if (fleet in dontSearchForMarket) return null

        fleet.addEventListener(this) // its getting added to a list no matter what

        for (market in Global.getSector().economy.marketsCopy) {
            if (market.getStationFleet() == fleet) {
                stationsToMarket[fleet] = market
                return market
            }
        }
        dontSearchForMarket += fleet // failed, itd be a waste to keep searching every time
        return null
    }

    fun getMarketOfFleet(fleet: CampaignFleetAPI): MarketAPI? {
        val cached = stationsToMarket[fleet]
        if (cached != null) return cached

        var memMarket: MarketAPI? = fleet.memoryWithoutUpdate[MemFlags.STATION_MARKET] as MarketAPI?
        if (memMarket == null && (fleet !in dontSearchForMarket)) memMarket = findMarket(fleet)
        if (memMarket != null) {
            stationsToMarket[fleet] = memMarket // cache
            if (fleet.eventListeners.none { it == this }) {
                fleet.addEventListener(this)
            }
            return memMarket
        }
        return null
    }

    override fun reportFleetDespawnedToListener(
        fleet: CampaignFleetAPI?,
        reason: CampaignEventListener.FleetDespawnReason?,
        param: Any?
    ) {

        if (fleet == null) return

        stationsToMarket -= fleet
        dontSearchForMarket -= fleet
        fleet.removeEventListener(this)
    }

    override fun reportBattleOccurred(fleet: CampaignFleetAPI?, primaryWinner: CampaignFleetAPI?, battle: BattleAPI?) {
        return
    }
}