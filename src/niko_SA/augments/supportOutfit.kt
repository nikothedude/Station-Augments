package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import niko_SA.genericIndustries.stationAttachment

class supportOutfit: stationAttachment() {

    companion object {
        const val FIGHTER_RANGE_PERCENT = 300f
        const val WEAPON_RANGE_PERCENT = 200f

        const val WEAPON_ROF_PERCENT = -20f
    }

    override val augmentCost: Float = 20f

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.fighterWingRange.modifyPercent(id, FIGHTER_RANGE_PERCENT)
            module.mutableStats.energyWeaponRangeBonus.modifyPercent(id, WEAPON_RANGE_PERCENT)
            module.mutableStats.ballisticWeaponRangeBonus.modifyPercent(id, WEAPON_RANGE_PERCENT)

            module.mutableStats.ballisticRoFMult.modifyPercent(id, WEAPON_ROF_PERCENT)
            module.mutableStats.energyRoFMult.modifyPercent(id, WEAPON_ROF_PERCENT)
        }
    }

    override fun apply() {
        return
    }
}