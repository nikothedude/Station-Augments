package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.CombatTaskManagerAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class commsCenter : stationAttachment() {

    companion object {
        //const val CP_REGEN_RATE = 750f
        //const val CP_INCREMENT = 10f

        const val ACCESSABILITY_INCREMENT = 0.1f
    }

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
            market?.accessibilityMod?.modifyFlat(id, ACCESSABILITY_INCREMENT, "${stationIndustry.currentName}: ${getName()}")
        }
    }

    override fun unapply() {
        super.unapply()

        market?.accessibilityMod?.unmodify(id)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Fleets defending the station in-combat have %s command points.",
            5f,
            Misc.getHighlightColor(),
            "unlimited"
        )
        tooltip.addPara(
            "Increases market accessibility by %s.",
            5f,
            Misc.getHighlightColor(),
            "${(ACCESSABILITY_INCREMENT * 100f).toInt()}%"
        )
    }
}