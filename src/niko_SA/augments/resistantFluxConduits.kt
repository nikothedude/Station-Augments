package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class resistantFluxConduits() : stationAttachment() {

    companion object {
        const val EMP_DAMAGE_MULT = 0.5f
        const val VENT_RATE_MULT = 1.25f
    }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.empDamageTakenMult.modifyMult(id, EMP_DAMAGE_MULT)
            module.mutableStats.ventRateMult.modifyMult(id, VENT_RATE_MULT)
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Decreases EMP damage taken by %s.",
            5f,
            Misc.getHighlightColor(),
            "${((1 - EMP_DAMAGE_MULT) * 100f).toInt()}%"
        )

        tooltip.addPara(
            "Increases flux dissipation while venting by %s.",
            5f,
            Misc.getHighlightColor(),
            "${((1 - VENT_RATE_MULT) * -100f).toInt()}%"
        )
    }
}