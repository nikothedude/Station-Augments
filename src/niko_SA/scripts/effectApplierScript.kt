package niko_SA.scripts

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.input.InputEventAPI
import niko_SA.MarketUtils.getStationAugments
import niko_SA.SA_ids.SA_structureTag
import niko_SA.augments.core.stationAttachment
import niko_SA.codex.CodexData

class effectApplierScript: BaseEveryFrameCombatPlugin() {

    override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
        val engine = Global.getCombatEngine()

        for (ship in engine.ships) {
            if (ship.isStation) {
                val member = ship.fleetMember ?: continue
                val battle = Global.getSector().playerFleet?.battle
                val fleet = battle?.memberSourceMap?.get(member) ?: member.fleetData?.fleet ?: continue
                val marketTracker = stationMarketTracker.getInstance()
                val market = marketTracker.getMarketOfFleet(fleet) ?: return
                for (augment in market.getStationAugments()) {
                    augment.applyInCombat(ship)
                    CodexData.unlockAugment(augment.id)
                }
            }
        }
        engine.removePlugin(this) // suicidal
    }
}