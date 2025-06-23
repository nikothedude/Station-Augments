package niko_SA.augments

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.CombatEngineLayers
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.missions.academy.GAProjectZiggurat.SCANNED_ZIGGURAT
import com.fs.starfarer.api.impl.combat.MoteControlScript
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getStationAugments
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color

open class moteSink(): stationAttachment(), EveryFrameScript {
    open val highVolatility: Boolean = true

    open val moteInterval = IntervalUtil(0.1f, 0.4f) // days
    var daysApplied = 0f

    companion object {
        fun get(market: MarketAPI): moteSink? = market.getStationAugments().firstOrNull { it is moteSink } as? moteSink
    }

    override fun applyInCombat(station: ShipAPI) {
        val engine = Global.getCombatEngine()
        val fleetManager = engine.getFleetManager(station.owner)
        fleetManager.isSuppressDeploymentMessages = true
        val specId = if (highVolatility) "SA_moteDrone_high" else "SA_moteDrone_low"
        val shieldDrone = fleetManager.spawnShipOrWing(specId, Vector2f(station.location), 0f)
        shieldDrone.spriteAPI.alphaMult = 0f
        shieldDrone.extraAlphaMult2 = 0f // invisible
        shieldDrone.isAlly = station.isAlly
        shieldDrone.isHoldFire = true
        shieldDrone.collisionClass = CollisionClass.FIGHTER
        shieldDrone.activeLayers.remove(CombatEngineLayers.FF_INDICATORS_LAYER)
        fleetManager.isSuppressDeploymentMessages = false

        shieldDrone.mutableStats.hullDamageTakenMult.modifyMult(id, 0f) // cant kill it
        shieldDrone.mutableStats.engineDamageTakenMult.modifyMult(id, 0f)

        //shieldDrone.aiFlags.setFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON)

        Global.getCombatEngine().addPlugin(DroneShieldLinker(shieldDrone, station))
    }

    class DroneShieldLinker(val drone: ShipAPI, val station: ShipAPI) : BaseEveryFrameCombatPlugin() {
        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            if (Global.getCombatEngine().isPaused) return

            drone.location.set(station.location.x, station.location.y)
            if (station.isHulk || !station.isAlive) {
                drone.mutableStats.hullDamageTakenMult.unmodify()
                Global.getCombatEngine().applyDamage(drone, Misc.ZERO, Float.MAX_VALUE, DamageType.ENERGY, 0f, true, false, null, false)
                val data = MoteControlScript.getSharedData(drone)
                data.motes.forEach {
                    it.flameOut()
                }
                data.motes.clear()
                Global.getCombatEngine().removeEntity(drone)
                Global.getCombatEngine().removePlugin(this)
            }
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        var descString = ""
        if (highVolatility) {
            if (Global.getSector().memoryWithoutUpdate[SCANNED_ZIGGURAT] == true) {
                tooltip.addPara(
                    "Much like the \"ziggurat\"-class ship you encountered prior, this modification seems to provide the station with a whorling " +
                    "mass of \"motes\", each giving off dangerously energetic readings.",
                    5f
                )
            } else {
                tooltip.addPara(
                    "This terrifying modification provides the the station with a whorling mass of \"motes\", each giving off dangerously energetic readings.",
                    5f
                )
            }
            descString = "surrounds it with a mass of motes that deal high EMP/energy damage through shields, as well as acting as PD"
        } else {
            tooltip.addPara(
                "Installing the damaged rift-core into a station can, at extreme cost, provide a mass of motes similar - yet weaker - than your previous encounter.",
                5f
            )

            descString = "surrounds it with a mass of motes that deal high EMP damage through shields, as well as acting as PD"
        }

        tooltip.addPara(
            "Adds %s as a bonus system to the station, which ${descString}.",
            5f,
            Misc.getHighlightColor(),
            "mote attractor"
        )

        if (!highVolatility) {
            val label = tooltip.addPara(
                "This augment is special: Only one station may have it applied, and it is unusually cheap for it's power.",
                5f,
                Misc.getGrayColor()
            )
            label.setHighlight("special", "unusually cheap for it's power")
            label.setHighlightColors(Misc.getHighlightColor(), Misc.getPositiveHighlightColor())
            label.setColor(Misc.getGrayColor())
        }
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    override fun apply() {
        super.apply()
        market?.primaryEntity?.addScript(this)
    }

    override fun unapply() {
        super.unapply()
        market?.primaryEntity?.removeScript(this)
        moteInterval.elapsed = 0f

        if (!reapplying) {
            daysApplied = 0f
        }
    }

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        daysApplied += days
        if (market?.containingLocation?.isCurrentLocation != true) return
        moteInterval.advance(days)
        if (!moteInterval.intervalElapsed()) return
        if (getStationIndustry()?.isDisrupted == true) return

        val entity = getStationCampaignEntity() ?: return
        spawnMote(entity, highVolatility)
    }

    open fun spawnMote(from: SectorEntityToken, volatile: Boolean = true) {
        if (!from.isInCurrentLocation) return
        var dur = 1f + 2f * Math.random().toFloat()
        dur *= 2f
        var size = 3f + Math.random().toFloat() * 5f
        size *= 3f
        val color = if (volatile) Color(255, 100, 255, 175) else Color(100,165,255,255)
        val loc = Misc.getPointWithinRadius(from.location, from.radius)
        val angle = MathUtils.getRandomNumberInRange(0f, 360f)
        val vel = Misc.getUnitVectorAtDegreeAngle(angle)
        vel.scale(15f + Math.random().toFloat() * 10f)
        Vector2f.add(vel, from.velocity, vel)
        Misc.addGlowyParticle(from.containingLocation, loc, vel, size, 0.5f, dur, color)
    }

    override fun getCombatDropChance(): Float {
        return 0f
    }
}