package niko_SA.augments

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BattleAPI
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_combatUtils
import niko_SA.augments.core.stationAttachment
import org.lwjgl.util.vector.Vector2f

/** Spawns a small collection of ships from the station itself on battle start. Frigates, maybe a destroyer. */
class defenseGarrison(market: MarketAPI?, id: String) : stationAttachment(market, id), EveryFrameScript {

    companion object {
        const val FLEET_POINTS = 100f

        // we regenerate to make sure we have the latest doctrine
        const val DAYS_BETWEEN_REGEN_UNDAMAGED = 100f
        const val DAYS_BETWEEN_REGEN_DAMAGED = 30f
    }

    override val name: String = "Emergency Defense Garrison"
    override val spriteId: String = "graphics/icons/industry/mining.png"

    override val augmentCost: Float = 20f

    var garrisonStartingStrength: Float = 0f
    var garrison: CampaignFleetAPI? = null

    val regenerationInterval = IntervalUtil(DAYS_BETWEEN_REGEN_UNDAMAGED, DAYS_BETWEEN_REGEN_UNDAMAGED)

    override fun apply() {
        super.apply()
        Global.getSector().addScript(this)

        if (garrison == null) {
            regenerateFleet()
        }
    }

    override fun unapply() {
        super.unapply()
        Global.getSector().removeScript(this)

        if (!reapplying) {
            garrison?.despawn()
        }
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (garrison == null) return

        // 1.2f here is to make it so simply losing cr doesnt cause a quick respawn
        if (garrison!!.isDespawning || ((garrison!!.effectiveStrength * 1.2f) < garrisonStartingStrength)) {
            val currProgress = regenerationInterval.elapsed
            regenerationInterval.setInterval(DAYS_BETWEEN_REGEN_DAMAGED, DAYS_BETWEEN_REGEN_DAMAGED)
            regenerationInterval.elapsed = currProgress
        }

        val days = Misc.getDays(amount)

        regenerationInterval.advance(days)
        if (regenerationInterval.intervalElapsed()) {
            regenerateFleet()
        }
    }

    fun regenerateFleet(): CampaignFleetAPI? {
        garrison?.despawn()
        garrison = createFleet()
        if (garrison != null) {
            garrisonStartingStrength = garrison!!.effectiveStrength
        }

        regenerationInterval.setInterval(DAYS_BETWEEN_REGEN_UNDAMAGED, DAYS_BETWEEN_REGEN_UNDAMAGED)

        return garrison
    }

    fun createFleet(): CampaignFleetAPI? {
        if (market == null) return null

        val fleetParams = FleetParamsV3(
            market,
            FleetTypes.PATROL_MEDIUM,
            FLEET_POINTS,
            0f,
            0f,
            0f,
            0f,
            0f,
            0f,
        )
        fleetParams.ignoreMarketFleetSizeMult = true

        return FleetFactoryV3.createFleet(fleetParams)
    }

    override fun applyInCombat(station: ShipAPI) {
        if (garrison == null || garrison!!.isEmpty) return
        val engine = Global.getCombatEngine()
        engine.addPlugin(DelayedGarrisonDeploymentScript(this, station)) // TODO: try removing once the stupid deployment bugs are fixed
    }

    fun deployGarrison(station: ShipAPI) {
        val engine = Global.getCombatEngine()
        val battle: BattleAPI? = Global.getSector().playerFleet.battle

        battle!!.join(garrison, battle.pickSide(station.fleetMember.fleetData.fleet))

        val fleetManager = engine.getFleetManager(station.owner)

        val spawnLoc = Vector2f(0f, 0f)

        val wasSuppressing = fleetManager.isSuppressDeploymentMessages
        fleetManager.isSuppressDeploymentMessages = true

        val facing = if (station.owner == 0) 90f else 270f
        val spawned: MutableList<ShipAPI> = ArrayList()

        //val oldPlayerShip = engine.playerShip
        for (member in garrison!!.fleetData.membersListCopy) {
            if (member.isFlagship) continue // TODO: REMOVE LATER THIS IS FOR TESTING
            val ship = fleetManager.spawnFleetMember(member, spawnLoc, facing, 1f)
            spawned += ship
            ship.isAlly = station.isAlly
            ship.owner = station.owner
            ship.originalOwner = station.originalOwner
            ship.setLaunchingShip(station)
            ship.setAnimatedLaunch()

            ship.mutableStats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult(id, 0f)
        }
        //engine.setPlayerShipExternal(oldPlayerShip)

        SA_combatUtils.moveToSpawnLocations(spawned)
        val setY = if (station.owner == 0) (500f) else (-500f)
        for (ship in spawned) {
            //ship.location.x += station.location.x
            ship.location.y = setY
        }
        //spawned.forEach { it.location.translate(station.location.x, station.location.y + translateY) }
        fleetManager.isSuppressDeploymentMessages = wasSuppressing

        /*engine.addPlugin(
            MPC_defenseGarrisonCollisionScript(
            shipOne,
            5f
        ))*/
    }

    class DelayedGarrisonDeploymentScript(val augment: defenseGarrison, val station: ShipAPI) : BaseEveryFrameCombatPlugin() {
        val ticksToWait = 10
        var ticksWaited = 0

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            ticksWaited++
            if (ticksWaited >= ticksToWait) {
                augment.deployGarrison(station)
                Global.getCombatEngine().removePlugin(this)
            }
        }

    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "A small detachment of combat ships is stationed within the station and will respond to any attacks on it with force.",
            5f
        )

        if (garrison != null) {
            if (garrison!!.isEmpty) {
                tooltip.addPara("The fleet has been %s.", 5f, Misc.getNegativeHighlightColor(), "defeated")
            } else {
                tooltip.addPara(
                    "The fleet's current strength is at %s.",
                    5f,
                    Misc.getHighlightColor(),
                    "${(garrison!!.effectiveStrength / garrisonStartingStrength) * 100}%"
                )
            }
        }
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