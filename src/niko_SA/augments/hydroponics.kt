package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.applyDeficitToProductionStatic
import niko_SA.augments.core.stationAttachment

class hydroponics() : stationAttachment() {
    companion object {
        const val FOOD_PROD = 2
        const val ORGANICS_PROD = 1
    }

    override fun applyInCombat(station: ShipAPI) {
        return
    }

    override fun apply() {
        super.apply()

        val industry = getStationIndustry() ?: return
        if (industry.isDisrupted) return
        industry.supply(id, Commodities.FOOD, FOOD_PROD, getName())
        industry.supply(id, Commodities.ORGANICS, ORGANICS_PROD, getName())

        checkDeficits(industry)
    }

    private fun checkDeficits(industry: Industry) {
        val deficits = getSupplyAndFuelDeficit(industry) ?: return
        if (deficits.one == null) {
            deficits.one = Commodities.CREW // in this case, we have no deficits so this works to unapply (i think)
        }
        val index = 9

        industry.applyDeficitToProductionStatic(index, deficits, Commodities.FOOD, Commodities.ORGANICS)
    }

    private fun getSupplyAndFuelDeficit(industry: Industry): com.fs.starfarer.api.util.Pair<String, Int>? {
        return industry.getMaxDeficit(Commodities.SUPPLIES, Commodities.CREW)
    }

    override fun unapply() {
        super.unapply()

        val industry = getStationIndustry() ?: return
        industry.getSupply(Commodities.FOOD).quantity.unmodify(id)
        industry.getSupply(Commodities.ORGANICS).quantity.unmodify(id)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Modular hydroponics basins are a surprising challenge to set-up without pre-collapse material and soil nanites. But it can be done.",
            5f
        )

        tooltip.addPara(
            "Increases station %s production by %s, and %s production by %s.",
            5f,
            Misc.getHighlightColor(),
            "food", FOOD_PROD.toString(), "organics", ORGANICS_PROD.toString()
        )

        tooltip.addPara(
            "Output is dependant on deficits.",
            5f,
            Misc.getGrayColor()
        ).setColor(Misc.getGrayColor())
    }
}