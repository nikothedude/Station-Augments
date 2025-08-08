package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.augments.core.stationAttachment

class supportOutfit() : stationAttachment() {

    companion object {
        const val FIGHTER_RANGE_PERCENT = 1500f
        const val WEAPON_RANGE_PERCENT = 800f
        const val VISION_INCREMENT = 4000f

        const val WEAPON_ROF_PERCENT = -20f
        const val MISSILE_ROF_PERCENT = -50f
    }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.fighterWingRange.modifyPercent(id, FIGHTER_RANGE_PERCENT)
            module.mutableStats.energyWeaponRangeBonus.modifyPercent(id, WEAPON_RANGE_PERCENT)
            module.mutableStats.ballisticWeaponRangeBonus.modifyPercent(id, WEAPON_RANGE_PERCENT)
            module.mutableStats.missileWeaponRangeBonus.modifyPercent(id, WEAPON_RANGE_PERCENT)

            module.mutableStats.beamPDWeaponRangeBonus.modifyPercent(id, -WEAPON_RANGE_PERCENT)
            module.mutableStats.nonBeamPDWeaponRangeBonus.modifyPercent(id, -WEAPON_RANGE_PERCENT)

            module.mutableStats.sightRadiusMod.modifyFlat(id, VISION_INCREMENT)

            module.mutableStats.ballisticRoFMult.modifyPercent(id, WEAPON_ROF_PERCENT)
            module.mutableStats.energyRoFMult.modifyPercent(id, WEAPON_ROF_PERCENT)
            module.mutableStats.missileRoFMult.modifyPercent(id, MISSILE_ROF_PERCENT)
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        val para = tooltip.addPara(
            "Increases fighter engagement range by %s. Increases non-PD weapon range by %s, and decreases non-PD firerate by %s. Missiles receive a %s reduction in ROF.",
            5f,
            Misc.getHighlightColor(),
            "${FIGHTER_RANGE_PERCENT.trimHangingZero()}%", "${WEAPON_RANGE_PERCENT.trimHangingZero()}%", "${(-WEAPON_ROF_PERCENT).trimHangingZero()}%", "${(-MISSILE_ROF_PERCENT).trimHangingZero()}%"
        )
        para.setHighlightColors(Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getNegativeHighlightColor())
    }
}