package niko_SA.augments.shrouded

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud
import com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect
import com.fs.starfarer.api.impl.combat.threat.EnergyLashSystemScript.DelayedCombatActionPlugin
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.TimeoutTracker
import com.fs.starfarer.api.util.WeightedRandomPicker
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.max
import kotlin.math.min
import niko_SA.SA_mathUtils.trimHangingZero

class ShroudedThunderhead: ShroudedAugment() {

    companion object {
        fun getData(ship: ShipAPI): ShroudedThunderheadAugmentData {
            val engine = Global.getCombatEngine()
            val key = DATA_KEY + "_" + ship.id
            var data = engine.customData[key] as ShroudedThunderheadAugmentData?
            if (data == null) {
                data = ShroudedThunderheadAugmentData()
                engine.customData.put(key, data)

                ship.addListener(ShroudedThunderheadAugDamageDealtMod(ship))
            }
            return data
        }

        const val DATA_KEY = "SA_ShroudedThunderheadAugment_data_key"
        
        const val MAX_RANGE: Float = 3000f

        const val RECENT_HIT_DUR: Float = 5f
        const val MAX_TIME_SINCE_RECENT_HIT: Float = 0.1f
        const val WEIGHT_PER_RECENT_HIT: Float = 1f
        const val MIN_TOTAL_RECENT_HIT_WEIGHT: Float = 20f
        const val MAX_TOTAL_RECENT_HIT_WEIGHT: Float = 100f
        const val MISFIRE_WEIGHT: Float = 100f

        const val MIN_REFIRE_DELAY: Float = 0.22f
        const val MAX_REFIRE_DELAY: Float = 0.44f
        const val REFIRE_RATE_MULT: Float = 1f
        
        const val FLUX_PER_DAMAGE: Float = 0.2f
        const val MIN_DAMAGE: Float = 600f
        const val MAX_DAMAGE: Float = 800f
        const val EMP_MULT: Float = 1.5f

        const val DAMAGE_MULT = 1f
    }

    class ShroudedThunderheadAugDamageDealtMod(val ship: ShipAPI): DamageDealtModifier {
        override fun modifyDamageDealt(
            param: Any?,
            target: CombatEntityAPI?, damage: DamageAPI,
            point: Vector2f?, shieldHit: Boolean
        ): String? {
            if (param is DamagingProjectileAPI) {
                val proj = param
                val spec = proj.explosionSpecIfExplosion
                if (spec != null && spec.collisionClassIfByFighter == CollisionClass.GAS_CLOUD) {
                    return null
                }
            } else if ((damage.isDps && !damage.isForceHardFlux) || damage.damage <= 0f) {
                return null
            }

            if (target != null) {
                val data = getData(ship)
                val hit = RecentHitData()
                hit.param = param
                hit.target = target
                hit.point = Vector2f(point)
                hit.damage = damage
                hit.shieldHit = shieldHit
                //data.recentHits.add(hit, 1.5f);
                data.recentHits.add(hit, RECENT_HIT_DUR)
            }
            return null
        }
    }

    class RecentHitData {
        var param: Any? = null
        var target: CombatEntityAPI? = null
        var point: Vector2f? = null
        var damage: DamageAPI? = null
        var shieldHit: Boolean = false
    }

    class ShroudedThunderheadAugmentData {
        var untilArc: Float = 0f
        var recentHits: TimeoutTracker<RecentHitData?> = TimeoutTracker<RecentHitData?>()

        fun hasRecentEnoughHits(): Boolean {
            for (curr in recentHits.getItems()) {
                val remaining = recentHits.getRemaining(curr)
                if (remaining >= RECENT_HIT_DUR - MAX_TIME_SINCE_RECENT_HIT) return true
            }
            return false
        }

        fun getHitProbability(): Float { // this is only used for how much the shroud glows, not ACTUALLY the hit probability
                var recent = recentHits.getItems().size * WEIGHT_PER_RECENT_HIT
                if (recent > MAX_TOTAL_RECENT_HIT_WEIGHT) {
                    recent = MAX_TOTAL_RECENT_HIT_WEIGHT
                }
                return recent / (recent + MAX_TOTAL_RECENT_HIT_WEIGHT * 0.5f)
            }

        fun pickRecentHit(): RecentHitData? {
            if (!hasRecentEnoughHits()) return null
            val picker = WeightedRandomPicker<RecentHitData?>()
            var totalHitFrequency = recentHits.getItems().size * WEIGHT_PER_RECENT_HIT
            if (totalHitFrequency < MIN_TOTAL_RECENT_HIT_WEIGHT) {
                totalHitFrequency = MIN_TOTAL_RECENT_HIT_WEIGHT
            }
            if (totalHitFrequency > MAX_TOTAL_RECENT_HIT_WEIGHT) {
                totalHitFrequency = MAX_TOTAL_RECENT_HIT_WEIGHT
            }
            for (curr in recentHits.getItems()) {
                val remaining = recentHits.getRemaining(curr)
                if (remaining < RECENT_HIT_DUR - MAX_TIME_SINCE_RECENT_HIT * 2f) continue
                picker.add(curr, 1f)
            }
            val numRecentEnoughHits = picker.getItems().size.toFloat()
            if (numRecentEnoughHits > 0f) {
                for (i in picker.getItems().indices) {
                    picker.setWeight(i, totalHitFrequency / numRecentEnoughHits)
                }
            }
            val misfire = RecentHitData()
            picker.add(misfire, MISFIRE_WEIGHT)
            return picker.pick()
        }
    }

    override fun applyInCombat(station: ShipAPI) {
        super.applyInCombat(station)

        Global.getCombatEngine().addPlugin(ShroudedThunderheadCombatPlugin(station, this))

        return
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Fires rift lightning bolts at locations recently hit by the station's weapons.", 0f
        )

        tooltip.addPara(
            "Bolts deal %s damage (%s EMP) and cost %s flux to launch. The probability of a bolt being fired increases with " +
                    "the number of hits landed in the last %s seconds. Bolts are not triggered by weapons doing soft-flux damage, " +
                    "such as beams.",
            5f,
            Misc.getHighlightColor(),
            "${(DAMAGE_MULT * MAX_DAMAGE).trimHangingZero()}", "${((DAMAGE_MULT * MAX_DAMAGE) * EMP_MULT).trimHangingZero()}",
                    "${(FLUX_PER_DAMAGE * (DAMAGE_MULT * MAX_DAMAGE)).trimHangingZero()}", RECENT_HIT_DUR.toInt().toString()
        )

        tooltip.addPara(
            "This augment is especially useful for stations with loadouts that provide excess flux dissipation.",
            5f,
        ).color = Misc.getGrayColor()
    }

    class ShroudedThunderheadCombatPlugin(
        val station: ShipAPI,
        val augment: ShroudedThunderhead
    ): BaseEveryFrameCombatPlugin() {
        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            val engine = Global.getCombatEngine() ?: return
            if (!station.isAlive) {
                engine.removePlugin(this)
                return
            }
            if (engine.isPaused) return

            for (ship in (station.childModulesCopy + station).filter { it.isAlive && it.allWeapons.isNotEmpty() }) {
                val data = getData(ship)

                val prob = data.getHitProbability()
                val shroud = DwellerShroud.getShroudFor(ship)
                shroud.getParams().flashProbability = min(0.1f + prob * 1.4f, 1f)

                //shroud.getParams().alphaMult = 0.5f; 
                data.untilArc -= amount * REFIRE_RATE_MULT
                if (data.untilArc <= 0f) {
                    val hasRecentHits = data.hasRecentEnoughHits()
                    if (hasRecentHits) {
                        val hit = data.pickRecentHit()
                        if (hit != null && hit.target != null) {
                            data.recentHits.remove(hit)
                            spawnLightning(ship, hit)
                        }
                        data.untilArc = MIN_REFIRE_DELAY + Math.random()
                            .toFloat() * (MAX_REFIRE_DELAY - MIN_REFIRE_DELAY)
                    }
                }
            }
        }

        fun spawnLightning(ship: ShipAPI, hit: RecentHitData) {
            val engine = Global.getCombatEngine()

            var from = ship.location
            val point = hit.point

            var dist = Misc.getDistance(from, point)

            if (dist > MAX_RANGE) return

            val mult = DAMAGE_MULT
            val damage = getDamage()
            val emp = damage * EMP_MULT

            if (FLUX_PER_DAMAGE > 0f) {
                val fluxCost = damage * FLUX_PER_DAMAGE
                if (ship.fluxTracker.maxFlux > 100f && !augment.deductFlux(ship, fluxCost)) {
                    return
                }
            }

            val shroud = DwellerShroud.getShroudFor(ship)
            if (shroud != null) {
                val angle = Misc.getAngleInDegrees(ship.location, point)
                from = Misc.getUnitVectorAtDegreeAngle(angle + 90f - 180f * Math.random().toFloat())
                from.scale(
                    (0.5f + Math.random()
                        .toFloat() * 0.25f) * shroud.getShroudParams().maxOffset * shroud.getShroudParams().overloadArcOffsetMult
                )
                Vector2f.add(ship.location, from, from)
            }


            val arcSpeed = RiftLightningEffect.RIFT_LIGHTNING_SPEED

            var params = EmpArcParams()
            params.segmentLengthMult = 8f
            params.zigZagReductionFactor = 0.15f
            //params.fadeOutDist = ship.getCollisionRadius() * 0.5f;
            params.fadeOutDist = 50f
            params.minFadeOutMult = 10f
            //		params.flickerRateMult = 0.7f;
            params.flickerRateMult = 0.3f

            //		params.flickerRateMult = 0.05f;
//		params.glowSizeMult = 3f;
//		params.brightSpotFullFraction = 0.5f;
            params.movementDurOverride = max(0.05f, dist / arcSpeed)

            val arcWidth = 40f + mult * 40f
            val explosionRadius = 40f + mult * 40f


            //Color color = weapon.getSpec().getGlowColor();
            val color = RiftLightningEffect.RIFT_LIGHTNING_COLOR
            var arc = engine.spawnEmpArcVisual(
                from, ship, point, null,
                arcWidth,  // thickness
                color,
                Color(255, 255, 255, 255),
                params
            ) as EmpArcEntityAPI
            arc.coreWidthOverride = arcWidth / 2f

            arc.setRenderGlowAtStart(false)
            arc.setFadedOutAtStart(true)
            arc.setSingleFlickerMode(true)

            val volume = 0.75f + 0.25f * mult
            val pitch = 1f + 0.25f * (1f - mult)
            Global.getSoundPlayer().playSound("rift_lightning_fire", pitch, volume, from, ship.velocity)

            if (shroud != null) {
                val shroudParams = shroud.getShroudParams()
                params = EmpArcParams()
                params.segmentLengthMult = 4f
                params.glowSizeMult = 4f
                params.flickerRateMult = 0.5f + Math.random().toFloat() * 0.5f
                params.flickerRateMult *= 1.5f


                //Color fringe = shroudParams.overloadArcFringeColor;
                val fringe = color
                val core = Color.white

                val thickness = shroudParams.overloadArcThickness


                //Vector2f to = Misc.getPointAtRadius(from, 1f);
                var angle = Misc.getAngleInDegrees(from, ship.location)
                angle = angle + 90f * (Math.random().toFloat() - 0.5f)
                val dir = Misc.getUnitVectorAtDegreeAngle(angle)
                dist = shroudParams.maxOffset * shroud.getShroudParams().overloadArcOffsetMult
                dist = dist * 0.5f + dist * 0.5f * Math.random().toFloat()
                //dist *= 1.5f;
                dist *= 0.5f
                dir.scale(dist)
                val to = Vector2f.add(from, dir, Vector2f())

                arc = engine.spawnEmpArcVisual(
                    from, ship, to, ship, thickness, fringe, core, params
                ) as EmpArcEntityAPI

                arc.coreWidthOverride = shroudParams.overloadArcCoreThickness
                arc.setSingleFlickerMode(false)
                //arc.setRenderGlowAtStart(false);
            }


            val explosionDelay = params.movementDurOverride * 0.8f
            Global.getCombatEngine().addPlugin(DelayedCombatActionPlugin(explosionDelay, object : Runnable {
                override fun run() {
                    val spec = DamagingExplosionSpec(
                        0.1f,  // duration
                        explosionRadius,  // radius
                        explosionRadius * 0.5f,  // coreRadius
                        damage,  // maxDamage
                        damage / 2f,  // minDamage
                        CollisionClass.PROJECTILE_NO_FF,  // collisionClass
                        CollisionClass.GAS_CLOUD,  // collisionClassByFighter - using to flag it as from this effect
                        3f,  // particleSizeMin
                        3f,  // particleSizeRange
                        0.5f,  // particleDuration
                        0,  // particleCount
                        Color(255, 255, 255, 0),  // particleColor
                        Color(255, 100, 100, 0) // explosionColor
                    )
                    spec.minEMPDamage = emp * 0.5f
                    spec.maxEMPDamage = emp

                    spec.damageType = DamageType.ENERGY
                    spec.isUseDetailedExplosion = false
                    spec.soundSetId = "rift_lightning_explosion"
                    spec.soundVolume = 0.5f + 0.5f * mult

                    val explosion = engine.spawnDamagingExplosion(spec, ship, point)


                    //explosion.addDamagedAlready(target);
                    //color = new Color(255,75,75,255);

                    //		float baseSize = 10f;
                    //
                    //		NEParams p = RiftCascadeMineExplosion.createStandardRiftParams(
                    //				color, baseSize);
                    //		//p.hitGlowSizeMult = 0.5f;
                    //		p.noiseMult = 6f;
                    //		p.thickness = 25f;
                    //		p.fadeOut = 0.5f;
                    //		p.spawnHitGlowAt = 1f;
                    //		p.additiveBlend = true;
                    //		p.blackColor = Color.white;
                    //		p.underglow = null;
                    //		p.withNegativeParticles = false;
                    //		p.withHitGlow = false;
                    //		p.fadeIn = 0f;
                    //		//p.numRiftsToSpawn = 1;
                    //
                    //		RiftCascadeMineExplosion.spawnStandardRift(explosion, p);
                    var color = RiftLightningEffect.RIFT_LIGHTNING_COLOR
                    color = Color(255, 75, 75, 255)
                    val p = RiftCascadeMineExplosion.createStandardRiftParams(
                        color, 14f + 6f * mult
                    )
                    p.fadeOut = 0.5f + 0.5f * mult
                    p.hitGlowSizeMult = 0.6f
                    p.thickness = 50f


                    //p.thickness = 25f;


                    //		p.hitGlowSizeMult = 0.5f;
                    //		p.thickness = 25f;
                    //		p.fadeOut = 0.25f;
                    RiftCascadeMineExplosion.spawnStandardRift(explosion, p)
                }
            }))
        }

        fun getDamage(): Float {
            val mult = DAMAGE_MULT
            return MIN_DAMAGE + (MAX_DAMAGE - MIN_DAMAGE) * mult
        }
    }
}