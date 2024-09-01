package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent

class heavyArmor(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    override val name: String = "Heavy Armor"
    override val spriteId: String = "graphics/icons/industry/mining.png"

    companion object {
        const val ARMOR_INCREMENT = 600f
        const val TURN_RATE_MULT = 0.75f
    }

    override val augmentCost: Float = 15f

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.armorBonus.modifyFlat(id, ARMOR_INCREMENT)
        }

        station.mutableStats.maxTurnRate.modifyMult(id, TURN_RATE_MULT)
        station.mutableStats.turnAcceleration.modifyMult(id, TURN_RATE_MULT)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        val test = tooltip.addPara(
            "Increases %s of all modules by %s, but decreases %s by %s.",
            5f,
            Misc.getHighlightColor(),
            "armor rating", "${ARMOR_INCREMENT.toInt()}", "station turn rate", toPercent(1 - TURN_RATE_MULT)
        )
        test.setHighlightColors(Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getHighlightColor())

    }
}