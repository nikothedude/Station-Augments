package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent

class stabilizedShields() : stationAttachment() {

    companion object {
        const val SHIELD_UPKEEP_MULT = 0.5f
    }

    init {
        incompatibleAugments += "SA_shieldShunt"
    }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.shieldUpkeepMult.modifyMult(id, SHIELD_UPKEEP_MULT)
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
            "Reduces shield upkeep of station modules by %s.",
            5f,
            Misc.getHighlightColor(),
            toPercent(SHIELD_UPKEEP_MULT)
        )

        tooltip.addPara(
            "Cannot be installed on high-tech stations due to their pre-existing shield optimizations.",
            5f
        ).color = Misc.getGrayColor()
    }
}