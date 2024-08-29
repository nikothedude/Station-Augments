package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import niko_SA.genericIndustries.stationAttachment
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import java.util.*

/** Spawns a small collection of ships from the station itself on battle start. Frigates, maybe a destroyer. */
class defenseGarrison: stationAttachment() {

    override val augmentCost: Float = 40f

    override fun applyInCombat(station: ShipAPI) {
        val engine = Global.getCombatEngine()

        val fleetManager = engine.getFleetManager(station.owner)

        val spawnLoc = Vector2f(station.location)
        spawnLoc.x += station.collisionRadius

        fleetManager.isSuppressDeploymentMessages = true
        val shipOne = fleetManager.spawnShipOrWing("vanguard_Attack", spawnLoc, 0f, 5f)
        shipOne.isAlly = station.isAlly
        fleetManager.isSuppressDeploymentMessages = false

        shipOne.setLaunchingShip(station)
        shipOne.setAnimatedLaunch()

        /*engine.addPlugin(
            MPC_defenseGarrisonCollisionScript(
            shipOne,
            5f
        ))*/
    }

    override fun apply() {
        return
    }

    override fun unapply() {
        super.unapply()
    }

    class MPC_defenseGarrisonCollisionScript(
        val ship: ShipAPI,
        val failsafeSeconds: Float
    ): BaseEveryFrameCombatPlugin() {
        val interval = IntervalUtil(failsafeSeconds, failsafeSeconds)

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            interval.advance(amount)
            if (interval.intervalElapsed()) {
                ship.collisionClass = CollisionClass.SHIP
                Global.getCombatEngine().removePlugin(this)
            }
        }

    }
}