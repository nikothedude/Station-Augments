package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import niko_SA.genericIndustries.stationAttachment

class heavyArmor: stationAttachment() {

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

    override fun apply() {
        TODO("Not yet implemented")
    }
}