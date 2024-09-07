package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent

class automatedRepairUnit(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    companion object {
        const val REPAIR_RATE_MULT = 1.5f
    }

    override val augmentCost: Float = 10f
    override val name: String = "Automated Repair Unit"
    override val spriteId: String = "graphics/hullmods/automated_repair_unit.png"

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.combatWeaponRepairTimeMult.modifyMult(id, REPAIR_RATE_MULT)
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "Increases module weapon repair rate by %s.",
            5f,
            Misc.getHighlightColor(),
            toPercent(REPAIR_RATE_MULT - 1)
        )
    }
}