package niko_SA.augments

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class stationRepairUnit() : stationAttachment(), EveryFrameScript {

    override fun applyInCombat(station: ShipAPI) {
        return
    }

    override fun apply() {
        super.apply()

        market?.primaryEntity?.addScript(this)
    }

    override fun unapply() {
        super.unapply()

        market?.primaryEntity?.removeScript(this)
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        val industry = getStationIndustry() ?: return
        if (!industry.isDisrupted) return
        industry.setDisrupted(industry.disruptedDays - Misc.getDays(amount))
    }

    override fun getUnavailableReason(): String? {
        if (getStationIndustry()?.isDisrupted == true) {
            return "Cannot be added to a disrupted station"
        }

        return super.getUnavailableReason()
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "An elaborate system of hull-foam dispensers, auto-bots, and repair arms allow the station to repair much faster.",
            5f
        )

        tooltip.addPara(
            "%s the time it takes for the station to recover from being disrupted.",
            5f,
            Misc.getHighlightColor(),
            "Halves"
        )

        val industry = getStationIndustry()
        if (!applied && industry?.isDisrupted == true) {
            tooltip.addPara(
                "The ${industry.currentName} is currently disrupted, meaning this augment %s until it is repaired.",
                5f,
                Misc.getNegativeHighlightColor(),
                "cannot be applied"
            ).setColor(Misc.getGrayColor())
        } else {
            tooltip.addPara(
                "Cannot be applied if the station is currently under repairs.",
                5f
            ).setColor(Misc.getGrayColor())
        }
    }
}