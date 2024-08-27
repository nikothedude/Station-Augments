package niko_SA.genericIndustries

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Tags

/** Industries of this type attempt to modify an existing station in combat, and potentially, in campaign.*/
abstract class stationAttachment: BaseIndustry() {

    override fun isAvailableToBuild(): Boolean {
        return (getStationIndustry() != null)
    }

    override fun getUnavailableReason(): String {
        val station = getStationIndustry() ?: return "No orbital station"
        return super.getUnavailableReason()
    }

    /** Returns the orbital station industry instance. Required to not be null for us to be buildable.*/
    fun getStationIndustry(): Industry? {
        for (industry in getMarket().industries) {
            if (industry.spec.hasTag(Tags.STATION)) {
                return industry
            }
        }
        return null
    }

    /** Returns the in-combat station entity we are affecting. Returns null if we're not in combat, or it doesnt exist. */
    fun getStationEntity(): ShipAPI? {
        if (Global.getCurrentState() != GameState.COMBAT) return null
        val engine = Global.getCombatEngine()

        for (ship in engine.ships) {
            if (ship.isStation) {
                val member = ship.fleetMember ?: continue
                val fleet = member.fleetData?.fleet ?: continue
                if (fleet.memoryWithoutUpdate[MemFlags.STATION_MARKET] == getMarket()) {
                    return ship
                }
            }
        }
        return null
    }

    /** Ran once at the beginning of combat. */
    abstract fun applyInCombat(station: ShipAPI)
}