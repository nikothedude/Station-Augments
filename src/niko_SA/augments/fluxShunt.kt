package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.genericIndustries.stationAttachment

/** Locked to midline stations - a big problem with them is the fact they never drop shields. */
class fluxShunt: stationAttachment() {

    override val augmentCost: Float = 20f
    override val stationTypeWhitelist: HashSet<String> = hashSetOf(Industries.ORBITALSTATION_MID, Industries.BATTLESTATION_MID, Industries.STARFORTRESS_MID)

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.hardFluxDissipationFraction.modifyFlat(id, 0.5f)
        }
    }

    override fun getNeededStationTypeText(): String {
        return "a midline station"
    }

    override fun apply() {
        return
    }

    override fun createTooltip(mode: Industry.IndustryTooltipMode?, tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltip(mode, tooltip, expanded)
        if (tooltip == null) return
        tooltip.addPara(
            "Applies %s to all station modules.",
            5f,
            Misc.getHighlightColor(),
            "flux shunt"
        )
    }
}