package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent

class reinforcedBulkheads() : stationAttachment() {

    companion object {
        const val HULL_MULT = 1.4f
    }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.hullBonus.modifyMult(id, HULL_MULT)
            val detachChance = module.mutableStats.dynamic.getStat(Stats.MODULE_DETACH_CHANCE_MULT).modifiedInt
            if (detachChance <= 100f) {
                module.mutableStats.dynamic.getStat(Stats.MODULE_DETACH_CHANCE_MULT).modifyMult(id, 0f)
            }
            module.mutableStats.breakProb.modifyMult(id, 0f)
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Increases hull integrity by %s.",
            5f,
            Misc.getHighlightColor(),
            toPercent((HULL_MULT - 1))
        )
        tooltip.addPara(
            "Destroyed modules will not break apart or detach from the parent station (excluding armor).",
            5f
        )
    }
}