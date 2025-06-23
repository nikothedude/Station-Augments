package niko_SA.augments.shrouded

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEntityAPI
import com.fs.starfarer.api.combat.EmpArcEntityAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud
import com.fs.starfarer.api.impl.combat.dweller.TenebrousExpulsionSystemScript
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_mathUtils.prob
import niko_SA.SA_settings
import org.dark.shaders.distortion.DistortionShader
import org.dark.shaders.distortion.RippleDistortion
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.CombatUtils.applyForce
import org.lwjgl.util.vector.Vector2f

class shroudedMantle: ShroudedAugment() {
    override fun applyInCombat(station: ShipAPI) {
        super.applyInCombat(station)

        Global.getCombatEngine().addPlugin(ShroudedMantleCombatPlugin(station))

        return
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Upon losing a module, the station will release a large pulse of \"demonic\" energy that pushes back - as well as partially flaming out - nearby ships. " +
            "Nearby projectiles are banished as well.", 0f
        )

        tooltip.addPara(
            "This effect is more pronounced on non-armor modules, with an additional effect of all modules losing %s at the time of the pulse, " +
                    "and an expulsion of abyssal matter that confuses hostile targeting systems.",
            5f,
            Misc.getHighlightColor(),
            "40% flux"
        )

        tooltip.addPara(
            "The mantle needs time to recover after a successful expulsion, with %s seconds for primary modules, and %s seconds for armor.",
            5f,
            Misc.getHighlightColor(),
            "twenty", "ten"
        )
    }

    class ShroudedMantleCombatPlugin(
        val station: ShipAPI
    ): BaseEveryFrameCombatPlugin() {
        val originalChildList = station.childModulesCopy

        val interval = IntervalUtil(0.1f, 0.11f)

        var cooldownLeft: Float = 0f
        var pulsePrepLeft: Float = 0f
        val prepLightningInterval = IntervalUtil(0.2f, 0.21f)
        var currentPulseTarget: Pair<ShipAPI, ModuleType>? = null

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            //val adjustedAmount = amount * Global.getCombatEngine().timeMult.modified

            if (Global.getCombatEngine().isPaused) return

            if (!station.isAlive) {
                Global.getCombatEngine().removePlugin(this)
                return
            }

            if (cooldownLeft > 0f) {
                for (module in originalChildList.toMutableSet()) {
                    if (!module.isAlive) {
                        originalChildList -= module
                    }
                }

                cooldownLeft -= amount
                cooldownLeft = cooldownLeft.coerceAtLeast(0f)
            }

            if (pulsePrepLeft > 0f) {
                pulsePrepLeft -= amount
                pulsePrepLeft = pulsePrepLeft.coerceAtLeast(0f)
                if (pulsePrepLeft <= 0f) {
                    finalizePulse()
                } else {
                    prepLightningInterval.advance(amount)
                    if (prepLightningInterval.intervalElapsed()) {
                        doPrepLightning()
                    }
                }
            }

            interval.advance(amount)
            if (interval.intervalElapsed()) {
                checkMantle()
            }
        }

        private fun finalizePulse() {
            if (!station.isAlive) return // sanity

            currentPulseTarget?.second?.performPulse(station)
            for (module in station.childModulesCopy + station) {
                val shroud = DwellerShroud.getShroudFor(module) ?: continue
                //shroud?.params?.removeMembersAboveMaintainLevel = true
                shroud.params.flashFrequency /= 2f
                shroud.params.numToFlash /= 20
                shroud.params.flashRadius /= 4
                shroud.params.flashRateMult = 0.25f
            }

            pulsePrepLeft = 0f // sanity
            currentPulseTarget = null

            Global.getCombatEngine().customData.remove("\$SA_shroudedMantleCharging_${station.id}")
        }

        private fun doPrepLightning() {
            for (module in (station.childModulesCopy + station)) {
                if (!prob(40f)) continue
                val shroud = DwellerShroud.getShroudFor(module) ?: return

                val randModule = (station.childModulesCopy + station).randomOrNull() ?: return

                randModule.exactBounds.update(randModule.location, randModule.facing)

                val randBoundOne = randModule.exactBounds.segments.randomOrNull() ?: return
                val randBoundTwo = randModule.exactBounds.segments.randomOrNull() ?: return

                val params = EmpArcEntityAPI.EmpArcParams()
                Global.getCombatEngine().spawnEmpArcVisual(
                    (Vector2f(randModule.location).translate(randBoundOne.p1.x / 2, randBoundOne.p1.y / 2)),
                    randModule,
                    (Vector2f(randModule.location).translate(randBoundTwo.p2.x / 2, randBoundTwo.p2.y / 2)),
                    randModule,
                    30f,
                    shroud.params.flashFringeColor,
                    shroud.params.flashCoreColor,
                    params
                )
            }
        }

        private fun checkMantle() {
            if (pulsePrepLeft > 0 || cooldownLeft > 0) return

            val bestModuleDestroyed = getBestModuleDestroyed()
            if (bestModuleDestroyed != null) {
                startMantle(bestModuleDestroyed)
            }
        }

        private fun startMantle(target: Pair<ShipAPI, ModuleType>) {
            val module = target.first
            val type = target.second

            val sound = Global.getSoundPlayer().playSound(type.sound, 1f, 6f, station.location, Misc.ZERO)
            pulsePrepLeft = type.pulsePrepSeconds
            cooldownLeft = type.cooldownSecs

            currentPulseTarget = target

            for (module in station.childModulesCopy + station) {
                val shroud = DwellerShroud.getShroudFor(module) ?: continue
                //shroud.params.removeMembersAboveMaintainLevel = false
                //shroud.addMembers((shroud.members.size * 0.35f).toInt())
                shroud.params.flashFrequency *= 2f
                //shroud.params.flashProbability = 1f
                shroud.params.numToFlash *= 20
                shroud.params.flashRadius *= 4f
                shroud.params.flashRateMult = 2f
            }

            Global.getCombatEngine().customData["\$SA_shroudedMantleCharging_${station.id}"] = true
        }

        private fun getBestModuleDestroyed(withClear: Boolean = true): Pair<ShipAPI, ModuleType>? {
            var bestType: Pair<ShipAPI, ModuleType>? = null
            for (module in originalChildList - station) { // minus implicitely makes a copy (i think)
                if (!module.isAlive) {
                    val type = ModuleType.getType(module)
                    if (bestType == null || bestType.second.tier < type.tier) {
                        bestType = Pair(module, type)
                    }

                    if (withClear) {
                        originalChildList -= module
                    }
                }
            }
            return bestType
        }
    }

    enum class ModuleType(val tier: Int, val cooldownSecs: Float, val sound: String, val pulsePrepSeconds: Float) {
        ARMOR(1, 10f, "system_darkened_gaze_activate", 2.2f) {
            override fun performPulse(station: ShipAPI) {
                val max = getMaxDistOfModules(station) ?: return
                val tip = (max.first.collisionRadius / 2) + max.second
                pushAwayEntities(
                    station,
                    2150f,
                    tip + 1250f,
                    tip + 5000f,
                    flameout = true
                )
            }
        },
        //SECONDARY,
        PRIMARY(2, 20f, "system_convulsive_lunge", 1.9f) {
            override fun performPulse(station: ShipAPI) {
                val max = getMaxDistOfModules(station) ?: return
                val tip = (max.first.collisionRadius / 2) + max.second
                pushAwayEntities(
                    station,
                    4400f,
                    tip + 1800f,
                    tip + 6250f,
                    flameout = true
                )

                for (module in station.childModulesCopy + station) {
                    module.fluxTracker.decreaseFlux(module.fluxTracker.maxFlux * 0.4f) // 40% flux is instantly dumped
                }

                var angle = 0f
                while (angle <= 360f) {
                    val vector = Misc.getUnitVectorAtDegreeAngle(angle)
                    TenebrousExpulsionSystemScript.fireShroudedEjecta(
                        station,
                        vector
                    )
                    angle += 30f
                }
            }
        };

        abstract fun performPulse(station: ShipAPI)

        companion object {
            fun getType(module: ShipAPI): ModuleType {
                if (module.hullSpec.hasTag(Tags.MODULE_HULL_BAR_ONLY)) {
                    return ARMOR
                }
                return PRIMARY
            }

            fun pushAwayEntities(focus: ShipAPI, force: Float, minRange: Float, maxRange: Float, friendlyToo: Boolean = true, flameout: Boolean = false) {
                val engine = Global.getCombatEngine()

                for (iter in engine.shipGrid.getCheckIterator(focus.location, maxRange, maxRange)) {
                    val entity = iter as? CombatEntityAPI ?: return
                    if (!friendlyToo && entity.owner == focus.owner) continue

                    val dist = Misc.getDistance(focus.location, entity.location)
                    val effectDist = (dist - minRange).coerceAtMost(0f)
                    val effectMaxRange = maxRange - minRange

                    val adjustedDist = (dist - minRange).coerceAtMost(0f)

                    var effectMult = 1 - (1 / (maxRange / adjustedDist)).coerceAtMost(1f)
                    if (effectMult <= 0f) continue

                    /*val pushDir = VectorUtils.getDirectionalVector(focus.location, iter.location)
                    pushDir.scale(force * effectMult)

                    val vel = entity.velocity.length()
                    if (vel > 100f) {
                        // too fast! slow down
                        entity.velocity.scale((0.4f / effectMult).coerceAtMost(1f))
                    }

                    Vector2f.add(pushDir, entity.velocity, entity.velocity)*/

                    val oldMass = entity.mass
                    entity.mass = oldMass.coerceAtMost(1250f) // we want to shove capitals away
                    applyForce(entity, VectorUtils.getDirectionalVector(focus.location, iter.location), (force * effectMult))
                    entity.mass = oldMass
                    if (flameout && entity is ShipAPI && entity.owner != focus.owner) {
                        val engines = entity.engineController
                        val percentOfEnginesToFlameOut = (0.3f * effectMult)
                        if (percentOfEnginesToFlameOut <= 0.05f) continue
                        val totalEngines = engines.shipEngines.size
                        for (engine in engines.shipEngines.shuffled()) {
                            if ((engines.computeDisabledFraction()) >= percentOfEnginesToFlameOut) break
                            engine.disable()
                        }
                    }
                }

                for (iter in engine.asteroidGrid.getCheckIterator(focus.location, maxRange, maxRange)) {
                    val entity = iter as? CombatEntityAPI ?: return
                    if (!friendlyToo && entity.owner == focus.owner) continue

                    val dist = Misc.getDistance(focus.location, entity.location)
                    val effectDist = (dist - minRange).coerceAtMost(0f)
                    val effectMaxRange = maxRange - minRange

                    val adjustedDist = (dist - minRange).coerceAtMost(0f)

                    var effectMult = 1 - (1 / (maxRange / adjustedDist))
                    if (effectMult <= 0f) continue

                    applyForce(entity, VectorUtils.getDirectionalVector(focus.location, iter.location), (force * effectMult))
                }

                for (projectile in engine.projectiles.filter { Misc.getDistance(focus.location, it.location) <= (minRange * 1.3f)}) {
                    if (projectile.owner == focus.owner) continue

                    engine.removeEntity(projectile) // gone
                }

                if (SA_settings.graphicsLibEnabled) {
                    val ripple = RippleDistortion(focus.location, Misc.ZERO)
                    ripple.intensity = 400f
                    ripple.size = maxRange * 1f
                    ripple.fadeInSize(1.4f)
                    ripple.fadeOutIntensity(0.4f)

                    DistortionShader.addDistortion(ripple)
                }
            }
            fun getMaxDistOfModules(station: ShipAPI): Pair<ShipAPI, Float>? {
                var moduleWithMaxDist: ShipAPI? = null
                var maxDist = 0f

                for (module in station.childModulesCopy.filter { it.isAlive }) {
                    val dist = MathUtils.getDistance(station.location, module.location)
                    if (dist > maxDist) {
                        moduleWithMaxDist = module
                        maxDist = dist
                    }
                }

                if (moduleWithMaxDist == null) return null

                return Pair(moduleWithMaxDist, maxDist)
            }
        }
    }
}