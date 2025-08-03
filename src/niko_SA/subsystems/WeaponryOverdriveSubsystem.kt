package niko_SA.subsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.util.Misc
import org.magiclib.subsystems.MagicSubsystem
import java.util.EnumSet
import kotlin.math.min

class WeaponryOverdriveSubsystem(ship: ShipAPI) : MagicSubsystem(ship) {

    override fun getBaseActiveDuration(): Float = 5f

    override fun getBaseCooldownDuration(): Float = 20f

    // lifted from AmmoFeedersSubsystem
    override fun shouldActivateAI(amount: Float): Boolean {
        val target = ship.shipTarget
        if (target != null) {
            var score = 0f

            if (target.fluxTracker.isOverloadedOrVenting) {
                score += 9f
            } else {
                score += target.fluxLevel * 6f
            }

            val dist = Misc.getDistance(ship.location, target.location)
            if (dist > aiData.getEngagementRange()) {
                score -= 3f
            } else {
                score += 3f
            }

            val avgRange = aiData.getAverageWeaponRange(false)
            score += min(avgRange / dist, 8f)

            return score > 10f
        }

        if (ship.areSignificantEnemiesInRange()) return true

        return false
    }

    override fun onActivate() {
        super.onActivate()

        Global.getSoundPlayer().playSound(
            "system_ammo_feeder",
            1f,
            1f,
            ship.location,
            Misc.ZERO
        )

        stats.ballisticWeaponFluxCostMod.modifyMult(this.getDisplayText(), 0.5f)
        stats.energyWeaponFluxCostMod.modifyMult(this.getDisplayText(), 0.5f)
        stats.ballisticRoFMult.modifyMult(this.getDisplayText(), 2f)
        stats.energyRoFMult.modifyMult(this.getDisplayText(), 2f)
    }

    override fun onFinished() {
        stats.ballisticWeaponFluxCostMod.unmodify(this.getDisplayText())
        stats.ballisticRoFMult.unmodify(this.getDisplayText())
        stats.energyWeaponFluxCostMod.unmodify(this.getDisplayText())
        stats.energyRoFMult.unmodify(this.getDisplayText())
    }

    override fun advance(amount: Float, isPaused: Boolean) {
        super.advance(amount, isPaused)

        val level = effectLevel
        val systemSpec = Global.getSettings().getShipSystemSpec("ammofeed")
        val types = EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY)
        if (state == State.ACTIVE || state == State.IN || state == State.OUT) {
            ship.setWeaponGlow(level, systemSpec.weaponGlowColor, types)
        }
    }

    override fun getDisplayText(): String? = "Weaponry Overdrive"
}