package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import niko_SA.augments.core.stationAttachment
import java.awt.Color
import java.util.*



class safetyOverrides(market: MarketAPI, id: String) : stationAttachment(market, id) {

    override val name: String = "Safety Shunt"
    override val spriteId: String = "graphics/icons/industry/mining.png"

    companion object {
        const val ROF_MULT = 1.6f

        val WEAPON_COLOR = Color(255,100,255,255)
        const val TURN_RATE_MULT = 1.5f

        const val RECOIL_MULT = 2f

        const val FLUX_DISSIPATION_MULT = 3f
    }

    override val augmentCost: Float = 20f

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.recoilPerShotMult.modifyMult(id, RECOIL_MULT)
            module.mutableStats.maxRecoilMult.modifyMult(id, RECOIL_MULT)

            module.mutableStats.turnAcceleration.modifyMult(id, TURN_RATE_MULT)
            module.mutableStats.maxTurnRate.modifyMult(id, TURN_RATE_MULT)

            module.mutableStats.peakCRDuration.modifyMult(id, 0.000013f)
            module.mutableStats.crLossPerSecondPercent.modifyFlat(id, 1f)

            module.mutableStats.ballisticRoFMult.modifyMult(id, ROF_MULT)
            module.mutableStats.energyRoFMult.modifyMult(id, ROF_MULT)

            module.mutableStats.fluxDissipation.modifyMult(id, FLUX_DISSIPATION_MULT)
            module.mutableStats.ventRateMult.modifyMult(id, 0f)

            module.setWeaponGlow(0.7f, WEAPON_COLOR,
                EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY))
        }
    }
}