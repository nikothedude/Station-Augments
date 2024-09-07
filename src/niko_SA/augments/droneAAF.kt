// drones dont use the AAF theyre given
/*package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.combat.ai.O0OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO
import com.fs.starfarer.combat.ai.attack.AttackAIModule
import com.fs.starfarer.combat.ai.movement.maneuvers.oO0O
import com.fs.starfarer.combat.ai.movement.oOOO
import com.fs.starfarer.combat.entities.Ship
import com.fs.starfarer.loading.SpecStore
import com.fs.starfarer.loading.specs.M
import niko_SA.ReflectionUtils
import niko_SA.ReflectionUtils.set
import niko_SA.augments.core.stationAttachment

class droneAAF(market: MarketAPI?, id: String) : stationAttachment(market, id) {
    override val stationTypeWhitelist: HashSet<String> = hashSetOf(
        Industries.STARFORTRESS,
        Industries.STARFORTRESS_MID,
    )

    override val augmentCost: Float = 20f
    override val name: String = "Drone AAF Retrofit"
    override val spriteId: String = "graphics/hullmods/missile_autoloader.png"

    override fun applyInCombat(station: ShipAPI) {
        val engine = Global.getCombatEngine()
        engine.addPlugin(DroneAAFScript(station, id))
    }

    override fun getNeededStationTypeText(): String {
        return "a low-tech or midline star fortress"
    }

    class DroneAAFScript(val station: ShipAPI, val id: String): BaseEveryFrameCombatPlugin() {
        val checkedDrones = HashSet<ShipAPI>()

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            for (drone in station.deployedDrones) {
                if (checkedDrones.contains(drone)) continue
                val shipAI = drone.ai
                if (shipAI != null) {
                    val AAFSpec = SpecStore.o00000(M::class.java, "ammofeed") // EVIL FUCKED UP CODE
                    val newSystem = AAFSpec.createSystem(drone as Ship?)

                    val threatEvalAI = ReflectionUtils.get("threatEvalAI", drone.ai)
                    val attackAI = ReflectionUtils.get("attackAI", drone.ai)
                    val flockingAI = ReflectionUtils.get("flockingAI", drone.ai)

                    set("system", drone, newSystem)
                    val newSystemAI = AAFSpec.createSystemAI(
                        drone, drone.aiFlags,
                        threatEvalAI as O0OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO?,
                        attackAI as AttackAIModule?,
                        flockingAI as oOOO?,
                        drone.ai as (oO0O.o) //ShipAI obf class
                    )

                    set("systemAI", shipAI, newSystemAI)
                }
                checkedDrones += drone
            }
        }
    }
}*/