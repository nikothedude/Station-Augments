package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.CombatFleetManagerAPI
import com.fs.starfarer.api.combat.CombatTaskManagerAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class commsCenter(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    companion object {
        //const val CP_REGEN_RATE = 750f
        //const val CP_INCREMENT = 10f

        const val ACCESSABILITY_INCREMENT = 0.1f
    }

    override val augmentCost: Float = 13f
    override val name: String = "Command Center"
    override val spriteId: String = "graphics/hullmods/operations_center.png"

    override fun applyInCombat(station: ShipAPI) {

        val engine = Global.getCombatEngine()
        val manager = engine.getFleetManager(station.originalOwner) ?: return

        val taskManagers = HashSet<CombatTaskManagerAPI>()
        taskManagers += manager.getTaskManager(false)
        if (station.owner == 0) {
            taskManagers += manager.getTaskManager(true)
        }

        for (taskManager in taskManagers) {
            taskManager.commandPointsStat.modifyFlat(id, 999999f) // INFINITE POWER
        }
    }

    override fun apply() {
        super.apply()

        val stationIndustry = getStationIndustry() ?: return
        if (stationIndustry.isFunctional) {
            market?.accessibilityMod?.modifyFlat(id, ACCESSABILITY_INCREMENT, "${stationIndustry.currentName}: $name")
        }
    }

    override fun unapply() {
        super.unapply()

        market?.accessibilityMod?.unmodify(id)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "Fleets defending the station in-combat have %s command points.",
            5f,
            Misc.getHighlightColor(),
            "unlimited"
        )
        tooltip.addPara(
            "Increases market accessability by %s.",
            5f,
            Misc.getHighlightColor(),
            "${(ACCESSABILITY_INCREMENT * 100f).toInt()}%"
        )
    }
}