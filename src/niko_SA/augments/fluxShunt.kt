package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

/** Locked to midline stations - a big problem with them is the fact they never drop shields. */
class fluxShunt() : stationAttachment() {

    override val stationTypeWhitelist: HashSet<String> = hashSetOf(Industries.ORBITALSTATION_MID, Industries.BATTLESTATION_MID, Industries.STARFORTRESS_MID)

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.hardFluxDissipationFraction.modifyFlat(id, 0.5f)
        }
    }

    override fun getNeededStationTypeText(): String {
        return "a midline station"
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)
        tooltip.addPara(
            "Applies %s to all station modules.",
            5f,
            Misc.getHighlightColor(),
            "flux shunt"
        )
    }

    override fun getBlueprintValue(): Int {
        return 30000
    }
}