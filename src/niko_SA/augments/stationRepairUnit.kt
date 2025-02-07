package niko_SA.augments

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class stationRepairUnit(market: MarketAPI?, id: String) : stationAttachment(market, id), EveryFrameScript {
    override val augmentCost: Float = 21f
    override val name: String = "Structural Repair Unit"
    override val spriteId: String = "graphics/augments/SA_stationRepairUnit.png"

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

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

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
    }
}