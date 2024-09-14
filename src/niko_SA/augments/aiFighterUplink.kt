package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.EveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ViewportAPI
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils

class aiFighterUplink(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    override val manufacturer: String = "Tri-Tachyon"
    override val augmentCost: Float = 8f
    override val name: String = "AI Fighter Uplink"
    override val spriteId: String = "graphics/hullmods/automated.png"

    override fun applyInCombat(station: ShipAPI) {
        val stationIndustry = getStationIndustry() ?: return
        val aiCoreId = stationIndustry.aiCoreId ?: return

        val engine = Global.getCombatEngine()

        for (module in station.childModulesCopy + station) {
            engine.addPlugin(AiFighterUplinkScript(module, market!!.factionId, module == station, aiCoreId))
        }
    }

    override fun getUnavailableReason(): String? {
        val superString = super.getUnavailableReason()
        if (superString != null) return superString

        val stationIndustry = getStationIndustry()!!
        val aiCore = stationIndustry.aiCoreId ?: return "No AI core installed in ${stationIndustry.currentName}"

        return null
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "Originally created in the 1st AI war, this nefarious comms module allows an AI core to directly interface" +
            " with fighters and drones launched from the station.",
            5f
        )

        tooltip.addPara(
            "If a star fortress, also allows for the drones to be controlled.",
            5f
        )
    }

    override fun getBlueprintValue(): Int {
        return 25000
    }

    class AiFighterUplinkScript(val module: ShipAPI, val factionId: String, val checkForDrones: Boolean = false, val aiCoreId: String): BaseEveryFrameCombatPlugin() {
        val interval = IntervalUtil(1f, 1.3f)

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            interval.advance(amount)
            if (interval.intervalElapsed()) {
                assignOfficers()
            }
        }

        private fun assignOfficers() {
            for (wing in module.allWings) {
                for (member in wing.wingMembers) {
                    if (member.captain != null) continue
                    member.captain = AICoreOfficerPluginImpl().createPerson(aiCoreId, factionId, MathUtils.getRandom())
                }
            }
            if (checkForDrones && module.deployedDrones != null) {
                for (drone in module.deployedDrones) {
                    if (drone.captain != null && drone.captain.isAICore) continue //drones have invisible captains, weirdly
                    drone.captain = AICoreOfficerPluginImpl().createPerson(aiCoreId, factionId, MathUtils.getRandom())
                }
            }
        }
    }
}