package niko_SA.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import niko_SA.SA_ids.SA_structureTag
import niko_SA.augments.core.stationAttachment

class effectApplierScript: BaseEveryFrameCombatPlugin() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val engine = Global.getCombatEngine()
        val battle = Global.getSector().playerFleet?.battle ?: return

        /*for (fleet in battle.bothSides) {
            val market = fleet.memoryWithoutUpdate[MemFlags.STATION_MARKET] as? MarketAPI ?: continue
            val stationMember = fleet.fleetData.membersListCopy.firstOrNull { it.isStation }
            battle.memberSourceMap
            for (industry in market.industries.filter { it.spec.hasTag(SA_structureTag) }) {
                val castedIndustry = (industry as stationAttachment)
                castedIndustry.applyInCombat(ship)
            }
        }*/

        for (ship in engine.ships) {
            if (ship.isStation) {
                val member = ship.fleetMember ?: continue
                val fleet = battle.memberSourceMap[member] ?: continue
                val marketTracker = stationMarketTracker.getInstance()
                val market = marketTracker.getMarketOfFleet(fleet) ?: return
                for (industry in market.industries.filter { it.spec.hasTag(SA_structureTag) }) {
                    val castedIndustry = (industry as stationAttachment)
                    castedIndustry.applyInCombat(ship)
                }
                //Global.getSector().addScript(stationMarketNullPatch(fleet, market)) // TEMPORARY MEASURE
            }
        }
        engine.removePlugin(this) // suicidal
    }
}