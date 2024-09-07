package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class supportOutfit(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    override val manufacturer: String = "Hegemony"
    override val name: String = "Support outfit"
    override val spriteId: String = "graphics/hullmods/integrated_targeting_unit.png"

    companion object {
        const val FIGHTER_RANGE_PERCENT = 1500f
        const val WEAPON_RANGE_PERCENT = 800f
        const val VISION_INCREMENT = 4000f

        const val WEAPON_ROF_PERCENT = -30f
    }

    override val augmentCost: Float = 20f

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.fighterWingRange.modifyPercent(id, FIGHTER_RANGE_PERCENT)
            module.mutableStats.energyWeaponRangeBonus.modifyPercent(id, WEAPON_RANGE_PERCENT)
            module.mutableStats.ballisticWeaponRangeBonus.modifyPercent(id, WEAPON_RANGE_PERCENT)

            module.mutableStats.beamPDWeaponRangeBonus.modifyPercent(id, -WEAPON_RANGE_PERCENT)
            module.mutableStats.nonBeamPDWeaponRangeBonus.modifyPercent(id, -WEAPON_RANGE_PERCENT)

            module.mutableStats.sightRadiusMod.modifyFlat(id, VISION_INCREMENT)

            module.mutableStats.ballisticRoFMult.modifyPercent(id, WEAPON_ROF_PERCENT)
            module.mutableStats.energyRoFMult.modifyPercent(id, WEAPON_ROF_PERCENT)
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        val para = tooltip.addPara(
            "Increases fighter engagement range by %s. Increases non-missile non-PD weapon range by %s, and decreases non-missile non-PD firerate by %s.",
            5f,
            Misc.getHighlightColor(),
            "$FIGHTER_RANGE_PERCENT%", "$WEAPON_RANGE_PERCENT%", "${-WEAPON_ROF_PERCENT}%"
        )
        para.setHighlightColors(Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getNegativeHighlightColor())
    }
}