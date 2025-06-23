package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils

class supportPackage() : stationAttachment() {

    companion object {
        const val MANEUVER_BONUS = 30f
        const val SPEED_BONUS = 15f
        const val RANGE_BONUS = 35f // real big honestly

        const val EFFECT_RANGE = 900f
        const val EFFECT_FADE = 1600f
    }

    override fun applyInCombat(station: ShipAPI) {
        var moduleWithMaxDist: CombatEntityAPI? = null
        var maxDist = 0f

        for (module in station.childModulesCopy) {
            val dist = MathUtils.getDistance(station.location, module.location)
            if (dist > maxDist) {
                moduleWithMaxDist = module
                maxDist = dist
            }
        }

        val radius = (if (moduleWithMaxDist != null) maxDist + moduleWithMaxDist.collisionRadius else station.collisionRadius)
        Global.getCombatEngine().addPlugin(SupportPackageScript(station, radius))
    }

    class SupportPackageScript(
        val station: ShipAPI,
        val stationRadWithModules: Float
    ): BaseEveryFrameCombatPlugin() {
        val interval = IntervalUtil(0.1f, 0.1f)

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            interval.advance(amount)
            val engine = Global.getCombatEngine()
            if (interval.intervalElapsed()) {
                for (ship in engine.ships) {
                    val mag = getPercentEffectiveness(ship)

                    applyEffect(ship, mag)
                }
            }
            val playerShip = engine.playerShip
            if (station.isAlive && playerShip != null && playerShip.owner == station.owner) {
                val icon = Global.getSettings().getSpriteName("ui", "icon_tactical_escort_package")
                val effectiveness = getPercentEffectiveness(playerShip)
                if (effectiveness > 0f) {
                    engine.maintainStatusForPlayerShip(
                        "SA_supportPackageKey",
                        icon,
                        "Tactical Link",
                        "${(effectiveness * 100f).toInt()}% telemetry quality",
                        false
                    )
                } else {
                    engine.maintainStatusForPlayerShip(
                        "SA_supportPackageKey",
                        icon,
                        "Tactical Link",
                        "No telemetry",
                        true
                    )
                }
            }
        }

        /** @return 0-1. */
        fun getPercentEffectiveness(ship: ShipAPI): Float {
            if (station.isHulk || !station.isAlive) return 0f
            if (ship.isFighter || ship.isHulk || ship.isShuttlePod || !ship.isAlive) return 0f
            if (ship.owner != station.owner) return 0f

            var radSum: Float = ship.shieldRadiusEvenIfNoShield + stationRadWithModules
            radSum *= 0.75f
            var dist = Misc.getDistance(ship.shieldCenterEvenIfNoShield, station.shieldCenterEvenIfNoShield)
            dist -= radSum

            var mag = 0f
            if (dist < EFFECT_RANGE) {
                mag = 1f
            } else if (dist < EFFECT_RANGE + EFFECT_FADE) {
                mag = 1f - (dist - EFFECT_RANGE) / EFFECT_FADE
            }

            return mag
        }

        fun applyEffect(ship: ShipAPI, mag: Float) {
            val id = "SA_support_package_bonus" + ship.id
            val stats = ship.mutableStats
            if (mag <= 0f) {
                stats.acceleration.unmodify(id)
                stats.deceleration.unmodify(id)
                stats.turnAcceleration.unmodify(id)
                stats.maxTurnRate.unmodify(id)

                stats.maxSpeed.unmodify(id)

                stats.ballisticWeaponRangeBonus.unmodify(id)
                stats.energyWeaponRangeBonus.unmodify(id)
            } else {
                val maneuver = MANEUVER_BONUS * mag
                stats.acceleration.modifyPercent(id, maneuver)
                stats.deceleration.modifyPercent(id, maneuver)
                stats.turnAcceleration.modifyPercent(id, maneuver * 2f)
                stats.maxTurnRate.modifyPercent(id, maneuver)

                val speed = SPEED_BONUS * mag
                stats.maxSpeed.modifyPercent(id, speed)

                val range = RANGE_BONUS * mag
                stats.ballisticWeaponRangeBonus.modifyPercent(id, range)
                stats.energyWeaponRangeBonus.modifyPercent(id, range)
            }
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Stations, due to their epicentric nature in combat, are perfect for coordinating tactical info and sensor telemetry across a defending force.",
            5f
        )

        tooltip.addPara(
            "Provides %s maneuverability and %s top speed to friendly ships within approximately %s su. Also increases ballistic and energy weapon range by %s.",
            5f,
            Misc.getHighlightColor(),
            "${MANEUVER_BONUS.toInt()}%", "${SPEED_BONUS.toInt()}%", "${EFFECT_RANGE.toInt()}", "${RANGE_BONUS.toInt()}%"
        )
    }
}