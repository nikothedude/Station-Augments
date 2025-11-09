package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_miscUtils
import niko_SA.SA_miscUtils.getFurthestModule
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f

class bubbleShield : stationAttachment() {

    companion object {
        const val SHIELD_STRENGTH = 100000f
        const val CONSTANT_DISSIPATION = 300f
        const val UNFOLD_RATE_MULT = 3f

        const val OVERLOAD_DURATION_MULT = 6.5f
    }

    override fun applyInCombat(station: ShipAPI) {
        val engine = Global.getCombatEngine()
        val fleetManager = engine.getFleetManager(station.owner)
        fleetManager.isSuppressDeploymentMessages = true
        val shieldDrone = fleetManager.spawnShipOrWing("wasp_Interceptor", Vector2f(station.location), 0f)
        //shieldDrone.spriteAPI.alphaMult = 0f
        //shieldDrone.extraAlphaMult2 = 0f // invisible
        shieldDrone.isAlly = station.isAlly
        shieldDrone.isHoldFire = true
        fleetManager.isSuppressDeploymentMessages = false

        shieldDrone.setShield(ShieldAPI.ShieldType.OMNI, 0f, 1f, 360f)
        shieldDrone.mutableStats.shieldUnfoldRateMult.modifyFlat(id, 1f)
        shieldDrone.mutableStats.fluxCapacity.modifyFlat(id, SHIELD_STRENGTH)
        shieldDrone.mutableStats.fluxDissipation.modifyFlat(id, CONSTANT_DISSIPATION)
        shieldDrone.mutableStats.hullDamageTakenMult.modifyMult(id, 0f) // cant kill it
        shieldDrone.mutableStats.shieldUnfoldRateMult.modifyMult(id, UNFOLD_RATE_MULT)
        shieldDrone.mutableStats.overloadTimeMod.modifyMult(id, OVERLOAD_DURATION_MULT)
        shieldDrone.mutableStats.hardFluxDissipationFraction.modifyFlat(id, 1f)
        shieldDrone.mutableStats.engineDamageTakenMult.modifyMult(id, 0f)
        shieldDrone.mutableStats.dynamic.getStat(Stats.SHIELD_PIERCED_MULT).modifyMult(id, 0f)
        shieldDrone.activeLayers.remove(CombatEngineLayers.FF_INDICATORS_LAYER)

        var moduleWithMaxDist: CombatEntityAPI? = station.getFurthestModule()
        var maxDist = moduleWithMaxDist?.let { MathUtils.getDistance(station.location, it.location) } ?: 0f

        var coloredShield = false
        for (module in station.childModulesCopy) {
            /*val dist = MathUtils.getDistance(station.location, module.location)
            if (dist > maxDist) {
                moduleWithMaxDist = module
                maxDist = dist
            }*/
            if (!coloredShield && module.shield != null) {
                shieldDrone.shield.ringColor = module.shield.ringColor
                shieldDrone.shield.innerColor = module.shield.innerColor
                coloredShield = true
            }
        }

        val shieldRadius = (if (moduleWithMaxDist != null) maxDist + moduleWithMaxDist.collisionRadius else station.collisionRadius) * 2.2f

        shieldDrone.shield.radius = shieldRadius
        shieldDrone.collisionRadius = shieldRadius * 1.1f

        shieldDrone.aiFlags.setFlag(ShipwideAIFlags.AIFlags.KEEP_SHIELDS_ON)

        Global.getCombatEngine().addPlugin(BubbleShieldLinker(shieldDrone, station))
        //module.collisionClass = CollisionClass.PROJECTILE_FIGHTER
    }

    class BubbleShieldLinker(val fxDrone: ShipAPI, val station: ShipAPI) : BaseEveryFrameCombatPlugin() {
        companion object {
            const val JITTER_STRENGTH_MULT = 1f
            const val MAX_JITTER_RANGE = 100f
        }

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)
            if (Global.getCombatEngine().isPaused) return

            if (fxDrone.fluxTracker.isOverloaded) {
                fxDrone.mutableStats.fluxDissipation.modifyMult("bubbleShieldOverloadDissipation", 500f)
            } else {
                fxDrone.mutableStats.fluxDissipation.unmodify("bubbleShieldOverloadDissipation")
            }

            fxDrone.location.set(station.location.x, station.location.y)
            fxDrone.shield.toggleOn() // idk why but its super hesitant to do this otherwise, even with the ai flag
            /*if (fxDrone.shield.isOff) {
                fxDrone.shield.toggleOn()
            }*/

            val fluxUsed = fxDrone.fluxTracker.fluxLevel
            val jitterIntensity = (fluxUsed * JITTER_STRENGTH_MULT)
            if (jitterIntensity > 0f) {
                val jitterRange = (MAX_JITTER_RANGE * fluxUsed)

                fxDrone.isJitterShields = true
                fxDrone.setJitter("bubbleShieldJitter", fxDrone.shield.innerColor, jitterIntensity, 1, jitterRange)
            }

            if (station.isHulk) {
                fxDrone.mutableStats.hullDamageTakenMult.unmodify()
                Global.getCombatEngine().removeEntity(fxDrone)
                Global.getCombatEngine().removePlugin(this)
            }
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "One of Fabrique Orbitale's more mundane inventions, the AREC-220 shield generator fits perfectly in the core of most stations " +
            "and projects a massive softshield around its host. Its intended use was to provide cover for defending ships, which are notably less durable than the station itself." +
            "Sadly, middling sales meant the end of the project - though it seems " +
            "a number of augspecs made their way into the sector despite the canning.",
            5f
        )

        val para = tooltip.addPara(
            "Projects a massive shield around the station, boasting %s, though with %s. " +
            "\n" +
            "The shield is managed by a high-level delta-AI, which modulates the shield to %s while %s.",
            5f,
            Misc.getHighlightColor(),
            "excellent flux capacity", "high overload downtime", "ignore ships", "blocking projectiles"
        )
        para.setHighlightColors(Misc.getPositiveHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getHighlightColor(), Misc.getHighlightColor())
    }

    override fun getBlueprintValue(): Int {
        return 25000
    }
}