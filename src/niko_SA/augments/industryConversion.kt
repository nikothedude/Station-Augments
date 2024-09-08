package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class industryConversion(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    companion object {
        const val INDUSTRY_INCREMENT = 1f

        const val HULL_MULT = 0.7f
        const val FLUX_CAPACITY_MULT = 0.7f
        const val FLUX_DISSIPATION_MULT = 0.8f
    }

    //override val manufacturer: String = "Ko Combine"
    override val augmentCost: Float = 30f
    override val name: String = "Economy Conversion"
    override val spriteId: String = "graphics/hullmods/converted_fighter_bay.png"

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.hullBonus.modifyMult(id, HULL_MULT)
            module.mutableStats.fluxCapacity.modifyMult(id, FLUX_CAPACITY_MULT)
            module.mutableStats.fluxDissipation.modifyMult(id, FLUX_DISSIPATION_MULT)
        }
    }

    override fun apply() {
        super.apply()

        val stationIndustry = getStationIndustry() ?: return
        if (stationIndustry.isFunctional) {
            market?.stats?.dynamic?.getMod(Stats.MAX_INDUSTRIES)?.modifyFlat(id, INDUSTRY_INCREMENT, "${stationIndustry.currentName}: $name")
        }
    }

    override fun unapply() {
        super.unapply()

        market?.stats?.dynamic?.getMod(Stats.MAX_INDUSTRIES)?.unmodify(id)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "Colonies in the core of the domain often found themselves overly protected. With the might of the domain behind them, " +
            "it wasn't too uncommon to repurpose large swaths of military infrastructure for more civilian use. This is one such modification.",
            5f
        )

        tooltip.addPara(
            "Allows for the construction of %s.",
            5f,
            Misc.getHighlightColor(),
            "an additional industry aboard the station"
        )

        tooltip.addPara(
            "The civilian conversion removes much of the military infrastructure aboard the station, reducing hull by %s, " +
            "flux capacity by %s, and flux dissipation by %s.",
            5f,
            Misc.getNegativeHighlightColor(),
            "${(((1 - HULL_MULT) * 100f)).toInt()}%", "${((1 - FLUX_CAPACITY_MULT) * 100f).toInt()}%", "${((1 - FLUX_DISSIPATION_MULT) * 100f).toInt()}%"
        )
    }

    override fun getBlueprintValue(): Int {
        return 5000
    }
}