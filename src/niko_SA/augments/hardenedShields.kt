package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.augments.core.stationAttachment

class hardenedShields: stationAttachment() {

    companion object {
        const val PIERCE_MULT: Float = 0.5f
        const val SHIELD_BONUS: Float = 20f
    }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.shieldDamageTakenMult.modifyMult(id, SHIELD_BONUS * 0.01f)
            module.mutableStats.dynamic.getStat(Stats.SHIELD_PIERCED_MULT).modifyMult(id, PIERCE_MULT)
        }
    }

    override fun getUnavailableReason(): String? {
        val superCall = super.getUnavailableReason()
        if (superCall != null) return superCall

        val industry = getStationIndustry() ?: return null
        if (industry.id == Industries.ORBITALSTATION_HIGH ||
            industry.id == Industries.BATTLESTATION_HIGH ||
            industry.id == Industries.STARFORTRESS_HIGH
        ) return "Cannot be installed on high-tech stations"

        return null
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Reduces the amount of damage taken by shields by %s. Also reduces the chance that shields will be pierced by EMP arcs from weapons like the Ion Beam.",
            5f,
            Misc.getHighlightColor(),
            "${SHIELD_BONUS.trimHangingZero()}%"
        )

        tooltip.addPara(
            "Cannot be installed on high-tech stations due to their pre-existing shield optimizations.",
            5f
        ).color = Misc.getGrayColor()
    }
}