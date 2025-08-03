package niko_SA.augments.threat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.GuidedMissileAI
import com.fs.starfarer.api.combat.OnFireEffectPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.threat.BaseFragmentMissileEffect
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

abstract class FragWeaponMimic(
    val weaponId: String,
    val fireSoundId: String,
    val ship: ShipAPI,
    var fragmentsNeeded: Float,
    var acceptableRange: Float = 0f,
    var doShipChecks: Boolean = true,
    var needTarget: Boolean = true
): BaseEveryFrameCombatPlugin() {
    val fakeWeapon = Global.getCombatEngine().createFakeWeapon(ship, weaponId)
    val spec = fakeWeapon.spec

    var fireDelay = fakeWeapon.refireDelay
    var delayLeft = fireDelay
    var autoFire = false

    var refireVarMin = 1f
    var refireVarMax = 1f

    var modifyHostShip: (ship: ShipAPI) -> Unit = { }
    var unModifyHostShip: (ship: ShipAPI) -> Unit = { }
    var bonusCanFireCheck: ((ship: ShipAPI, swarm: RoilingSwarmEffect) -> Boolean)? = null
    var bonusCanFireCheckTarget: ((ship: ShipAPI, swarm: RoilingSwarmEffect) -> Boolean)? = null

    data class fireParams(
        val location: Vector2f,
        val acceptableRange: Float,
        val doShipChecks: Boolean = true,
        var needTarget: Boolean = true
    )

    fun tryFiring(params: fireParams): Boolean {
        if (delayLeft > 0f) return false
        val swarm = RoilingSwarmEffect.getSwarmFor(ship) ?: return false
        if (swarm.members.size < fragmentsNeeded) return false
        if (bonusCanFireCheck?.invoke(ship, swarm) == false) return false

        val target = getFireTarget(params)
        if (!params.needTarget || target != null) {
            fire(target, params)
            return true
        }
        return false
    }

    open fun getFireTarget(params: fireParams): ShipAPI? {
        val swarm = RoilingSwarmEffect.getSwarmFor(ship) ?: return null

        val engine = Global.getCombatEngine()

        if (params.doShipChecks) {
            val picker = WeightedRandomPicker<ShipAPI>()

            for (ship in engine.shipGrid.getCheckIterator(
                params.location,
                params.acceptableRange,
                params.acceptableRange
            )) {
                if (ship !is ShipAPI) continue
                if (!ship.isAlive) continue
                if (ship.owner == this.ship.owner) {
                    continue
                } else {
                    var testvar = ""
                }
                if (bonusCanFireCheckTarget?.invoke(ship, swarm) == false) continue

                picker.add(ship, getWeightForShip(ship))
            }

            return picker.pick()
        }
        return null
    }

    protected open fun getWeightForShip(ship: ShipAPI): Float {
        if (ship.isFighter) return 5f
        val base = 20f
        if (ship.fluxTracker.isOverloadedOrVenting) {
            return base * 3f
        }
        return base
    }

    protected open fun fire(target: ShipAPI?, params: fireParams) {
        val engine = Global.getCombatEngine()
        val plugin = fakeWeapon.effectPlugin as? OnFireEffectPlugin ?: return

        modifyHostShip(ship)

        val proj = engine.spawnProjectile(
            ship,
            fakeWeapon,
            weaponId,
            fakeWeapon.location,
            ship.facing,
            ship.velocity
        ) as DamagingProjectileAPI
        val ai = proj.ai as? GuidedMissileAI
        if (ai != null && target != null) {
            ai.target = target
        }
        plugin.onFire(
            proj,
            fakeWeapon,
            engine
        )

        Global.getSoundPlayer().playSound(
            fireSoundId,
            1f,
            1f,
            fakeWeapon.location,
            ship.velocity
        )

        delayLeft = fireDelay * MathUtils.getRandomNumberInRange(refireVarMin, refireVarMax)

        unModifyHostShip(ship)
    }

    override fun advance(amount: Float, events: List<InputEventAPI?>?) {
        super.advance(amount, events)

        val engine = Global.getCombatEngine()
        if (!ship.isAlive) {
            engine.removePlugin(this)
            return
        }

        if (engine.isPaused) return

        fakeWeapon.location.set(ship.location)

        delayLeft = (delayLeft - amount).coerceAtLeast(0f)

        if (autoFire) {
            tryFiring(fireParams(
                ship.location,
                acceptableRange,
                doShipChecks,
                needTarget
            ))
        }
    }
}