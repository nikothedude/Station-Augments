package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getAugmentBudget
import niko_SA.MarketUtils.getRemainingAugmentBudget
import niko_SA.augments.core.stationAttachment
import kotlin.math.abs

class ExtremeModifications: stationAttachment() {

    companion object {
        const val HULL_AND_ARMOR_MALUS = -30f
        const val SHIELD_EFF_MALUS = -30f
        const val UPKEEP_BOOST = 1.3f
    }

    override var apToMemberStrengthMult: Float = 0f

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.hullBonus.modifyPercent(id, HULL_AND_ARMOR_MALUS)
            module.mutableStats.armorBonus.modifyPercent(id, HULL_AND_ARMOR_MALUS)
            module.mutableStats.shieldDamageTakenMult.modifyPercent(id, SHIELD_EFF_MALUS)
        }
    }

    override fun apply() {
        super.apply()

        val industry = getStationIndustry() ?: return
        if (market != null && applied) {
            industry.upkeep.modifyMult(id, UPKEEP_BOOST, getName())
        }
    }

    override fun unapply() {
        super.unapply()

        val industry = getStationIndustry() ?: return
        industry.upkeep.unmodify(id)
    }

    override fun canBeRemoved(): Boolean {
        if (!super.canBeRemoved()) return false
        
        val industry = getStationIndustry() ?: return true
        if ((industry.getRemainingAugmentBudget() + getAugmentCost()) < 0f) {
            return false
        }
        
        return true
    }
    
    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        val APcost = abs(getAugmentCost())
        tooltip.addPara(
            "Refits the station to be extremely modular, boosting AP by %s. This comes at the cost of the station's structural integrity, " +
        "reducing hull and armor integrity by %s, shield efficiency by %s, and increasing upkeep by %s.",
            0f,
            Misc.getHighlightColor(),
            "${APcost.toInt()}",
            "${-HULL_AND_ARMOR_MALUS.toInt()}%",
            "${-SHIELD_EFF_MALUS.toInt()}%",
            "${UPKEEP_BOOST}x"
        )

        tooltip.addPara(
            "Cannot be removed if doing such would put the AP budget below 0.",
            5f
        ).color = Misc.getGrayColor()
    }
}