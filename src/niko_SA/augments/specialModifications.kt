package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getRemainingAugmentBudget
import niko_SA.augments.core.stationAttachment
import niko_SA.SA_mathUtils.trimHangingZero

class specialModifications: stationAttachment() {

    companion object {
        const val HAZARD_RATING_DECREASE = 0.01f
        const val MALFUNCTION_CHANCE = 0.05f
        const val FLUX_DISSIPATION_PERCENT_REDUCTION = 20f
    }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.fluxDissipation.modifyPercent(id, -FLUX_DISSIPATION_PERCENT_REDUCTION)
            module.mutableStats.weaponMalfunctionChance.modifyFlat(id, MALFUNCTION_CHANCE)
        }
    }

    override fun apply() {
        super.apply()

        val industry = getStationIndustry() ?: return
        if (industry.isFunctional) {
            market?.hazard?.modifyFlat(id, -HAZARD_RATING_DECREASE, "${industry.currentName}: ${getName()}")
        }
    }

    override fun unapply() {
        super.unapply()

        market?.hazard?.unmodify(id)
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

        tooltip.addPara(
            "During a tour of Sindria's Great Battlestation, the Supreme Executor was distraught to discover the horribly unsafe conditions " +
            "combat engineers operated within. At any moment, flux-conduits can rupture, a ammo-rack can cook off, or they may be exposed to the void of space.",
            5f
        )
        tooltip.addPara(
            "In the midst of the diktat's Great Rearming, the diktat's chief engineer recommended a series of modifications that would make " +
            "stations far more \"safe\" to operate on, including an installation of blast-proof shielding sourced from the forge-vats of Sindria itself.",
            5f
        )
        tooltip.addPara(
            "Many a patriot will thank this doctrine, as just as many combat engineers will feverishly agree that they would not like their battlestation " +
            "Any Other Way. If the ammoracks are harder to reach, or a wrench is too tightly fastened to pick up, that is a small price to pay to save the lives of " +
            "loyal soldiers.",
            5f
        )
        tooltip.addPara(
            "Weapons have a %s chance to %s in-combat. Additionally, flux dissipation is reduced by %s.",
            10f,
            Misc.getNegativeHighlightColor(),
            "low", "malfunction", "${(FLUX_DISSIPATION_PERCENT_REDUCTION).trimHangingZero()}%"
        )
        tooltip.addPara(
            "On the brighter side, the neo-laminated plating reduces hazard rating by a whopping %s, and the removal of all non-essential itemry (while bad for morale) " +
            "%s.",
            5f,
            Misc.getPositiveHighlightColor(),
            "one percent", "slightly improves AP budget"
        )
    }

    override fun getBlueprintValue(): Int {
        return (super.getBlueprintValue() * 1.5f).toInt()
    }
}