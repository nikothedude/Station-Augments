package niko_SA.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.input.InputEventAPI
import niko_SA.SA_ids.SA_structureTag
import niko_SA.genericIndustries.stationAttachment

class niko_SA_effectApplierScript: BaseEveryFrameCombatPlugin() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val engine = Global.getCombatEngine()

        val shipsToScan = HashSet<ShipAPI>()

        for (ship in engine.ships) {
            if (ship.isStation) {
                val member = ship.fleetMember ?: continue
                val fleet = member.fleetData?.fleet ?: continue
                if (fleet.memoryWithoutUpdate[MemFlags.STATION_MARKET] == null) continue
                val market = fleet.memoryWithoutUpdate[MemFlags.STATION_MARKET] as? MarketAPI ?: continue
                for (industry in market.industries.filter { it.spec.hasTag(SA_structureTag) }) {
                    val castedIndustry = (industry as stationAttachment)
                    castedIndustry.applyInCombat(ship)
                }
            }
        }
        engine.removePlugin(this) // suicidal
    }
}