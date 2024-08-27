package niko_SA.genericIndustries

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.util.IntervalUtil

/** Spawns a small collection of ships from the station itself on battle start. Frigates, maybe a destroyer. */
class defenseGarrison: stationAttachment() {

    override fun applyInCombat(station: ShipAPI) {
        val engine = Global.getCombatEngine()
        val fleetManager = engine.getFleetManager(station.owner)
        val shipOne = fleetManager.spawnShipOrWing("vanguard_Attack", station.location, 0f, 5f)
        //val shipTwo = fleetManager.spawnShipOrWing("vanguard_Attack", station.location, 30f, 5f)
        //fleetManager.spawnShipOrWing("vanguard_Attack", station.location, 330f, 5f)

        shipOne.setLaunchingShip(station)
        shipOne.setAnimatedLaunch()

        shipOne.collisionClass = CollisionClass.FIGHTER

        engine.addPlugin(MPC_defenseGarrisonCollisionScript(
            shipOne,
            5f
        ))
    }

    override fun apply() {
        return
    }

    class MPC_defenseGarrisonCollisionScript(
        val ship: ShipAPI,
        val timeToWaitSeconds: Float
    ): BaseEveryFrameCombatPlugin() {
        val interval = IntervalUtil(timeToWaitSeconds, timeToWaitSeconds)

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            interval.advance(amount)
            if (interval.intervalElapsed()) {
                ship.collisionClass = CollisionClass.SHIP
                Global.getCombatEngine().removePlugin(this)
            }
        }

    }
}