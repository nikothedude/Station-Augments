package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.TemporalShellStats
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent
import java.awt.Color

class fighterTimeflow(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    companion object {
        const val TIMEFLOW_INCREMENT = 1.3f

        val JITTER_COLOR = Color(90, 165, 255, 55)
        val JITTER_UNDER_COLOR = Color(90, 165, 255, 155)
    }

    override val manufacturer: String = "Tri-Tachyon"
    override val augmentCost: Float = 20f
    override val name: String = "Fighter Temporal Cores"
    override val spriteId: String = "graphics/hullmods/temporal_shell.png"

    override fun applyInCombat(station: ShipAPI) {
        val engine = Global.getCombatEngine()
        engine.addPlugin(FighterTimeflowEffectScript(station, id))
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "While exceptionally unholy, temporal cores - derived from the scarab - may be manufactured on-the-fly and attached to " +
            "fighters and drones before they launch.",
            5f
        )

        tooltip.addPara(
            "Increases timeflow of %s and %s launched by the station by %s.",
            5f,
            Misc.getHighlightColor(),
            "fighters", "drones", toPercent((TIMEFLOW_INCREMENT - 1))
        )
    }

    class FighterTimeflowEffectScript(val station: ShipAPI, val id: String) : BaseEveryFrameCombatPlugin() {
        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            val shipsToAlter = HashSet<ShipAPI>()

            for (module in station.childModulesCopy + station) {
                for (wing in module.allWings) {
                    shipsToAlter.addAll(wing.wingMembers)
                }
            }
            if (station.deployedDrones != null) {
                for (drone in station.deployedDrones) {
                    shipsToAlter += drone
                    drone.mutableStats.timeMult.modifyMult(id, TIMEFLOW_INCREMENT)
                }
            }

            for (ship in shipsToAlter) {
                ship.mutableStats.timeMult.modifyMult(id, TIMEFLOW_INCREMENT)

                ship.setJitter(id, JITTER_COLOR, 0.8f, 2, 0f, 1f)
                ship.setJitterUnder(id, JITTER_UNDER_COLOR, 0.8f, 20, 0f, 5f)
            }
        }
    }
}