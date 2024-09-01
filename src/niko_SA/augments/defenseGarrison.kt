package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil
import niko_SA.augments.core.stationAttachment
import org.lwjgl.util.vector.Vector2f

/** Spawns a small collection of ships from the station itself on battle start. Frigates, maybe a destroyer. */
class defenseGarrison(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    override val name: String = "Defense Garrison"
    override val spriteId: String = "graphics/icons/industry/mining.png"

    override val augmentCost: Float = 20f

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