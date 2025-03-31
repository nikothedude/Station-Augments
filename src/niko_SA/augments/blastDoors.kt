package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import niko_SA.augments.core.stationAttachment

class blastDoors: stationAttachment() {

    companion object {
        const val HULL_BONUS = 20f
    }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.hullBonus.modifyPercent(id, HULL_BONUS)
        }
    }
}