package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent

class stabilizedShields(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    companion object {
        const val SHIELD_UPKEEP_MULT = 0.5f
    }

    init {
        incompatibleAugments += "SA_shieldShunt"
    }

    override val augmentCost: Float = 8f
    override val name: String = "Stabilized Shielding"
    override val spriteId: String = "graphics/hullmods/stabilized_shields.png"

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.shieldUpkeepMult.modifyMult(id, SHIELD_UPKEEP_MULT)
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        tooltip.addPara(
            "Reduces shield upkeep of station modules by %s.",
            5f,
            Misc.getHighlightColor(),
            toPercent(SHIELD_UPKEEP_MULT)
        )
    }
}