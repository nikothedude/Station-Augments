package niko_SA.augments.shrouded

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.combat.EmpArcEntityAPI.EmpArcParams
import com.fs.starfarer.api.impl.combat.RiftCascadeMineExplosion
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud
import com.fs.starfarer.api.impl.combat.dweller.RiftLightningEffect
import com.fs.starfarer.api.impl.combat.dweller.ShroudedLensHullmod
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.loading.DamagingExplosionSpec
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import niko_SA.SA_mathUtils.prob
import niko_SA.augments.jumpPoint.jumpPointCreator.Companion.ACCESSIBILITY_INCREMENT
import niko_SA.stringUtils
import org.lazywizard.lazylib.MathUtils
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.max

class ShroudedLens: ShroudedAugment() {

    companion object {
        fun getData(ship: ShipAPI): ShroudedLensAugmentData {
            val engine = Global.getCombatEngine()
            val key = DATA_KEY + "_" + ship.id
            var data = engine.customData.get(key) as ShroudedLensAugmentData?
            if (data == null) {
                data = ShroudedLensAugmentData()
                engine.customData.put(key, data)
            }
            return data
        }

        fun getRof(): Float {
            return MIN_ROF_MULT + (MAX_ROF_MULT - MIN_ROF_MULT) * STATION_POWER_MULT
        }

        const val DATA_KEY = "SA_ShroudedLensAugment_data_key"
        const val STATION_POWER_MULT = 1.2f

        const val MIN_ROF_MULT = 1f
        const val MAX_ROF_MULT = 4f

        const val MIN_REFIRE_DELAY = 0.7f
        const val MAX_REFIRE_DELAY = 0.8f

        const val MAX_RANGE_ON_TOP_OF_RADIUS = 900f

        const val FLUX_PER_DAMAGE = 0.75f
        const val DAMAGE = 50f

        const val RADIUS = 50f

        const val TIMES_TO_STRIKE_MIN = 1
        const val TIMES_TO_STRIKE_MAX = 3

        const val ACCESSIBILITY_MALUS = 0.1f

        /** Added on top of the amount of times to strike if our shrouded mantle is charging. */
        const val MANTLE_CHARGE_STRIKE_INCREMENT = 5
    }

    override fun applyInCombat(station: ShipAPI) {
        super.applyInCombat(station)

        Global.getCombatEngine().addPlugin(ShroudedLensAugmentPlugin(station, this))
    }

    override fun apply() {
        super.apply()

        if (market == null) return
        val industry = getStationIndustry() ?: return
        if (industry.isFunctional) {
            market?.accessibilityMod?.modifyFlat(id, ACCESSIBILITY_INCREMENT, "${industry.currentName}: ${getName()}")
        }
    }

    override fun unapply() {
        super.unapply()

        if (market == null) return
        market!!.accessibilityMod.unmodify(id)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "The \"lens\" (in the loosest sense of the word) focuses on nearby objects, " +
                "seemingly at random. Deals %s damage and generates %s flux.", 0f,
            Misc.getHighlightColor(),
            DAMAGE.toInt().toString(),
            (FLUX_PER_DAMAGE * DAMAGE).toInt().toString()
        )

        tooltip.addPara(
            "Fires exceptionally fast, and has a small chance to lash out at friendly objects.",
            5f,
        )

        tooltip.addPara(
            "If a %s is installed, the rate of fire is massively increased while the mantle roils from a recent loss.",
            5f,
            Misc.getHighlightColor(),
            "Shrouded Mantle"
        )

        tooltip.addPara(
            "Reduces market accessibility by %s.",
            5f,
            Misc.getNegativeHighlightColor(),
            stringUtils.toPercent(ACCESSIBILITY_MALUS)
        )
    }

    class ShroudedLensAugmentData {
        var untilAttack = 0f
        var sinceAttack = 1000f
    }

    class ShroudedLensAugmentPlugin(
        val station: ShipAPI,
        val masterAugment: ShroudedLens
    ): BaseEveryFrameCombatPlugin() {
        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            val engine = Global.getCombatEngine()
            if (!station.isAlive) {
                engine.removePlugin(this)
                return
            }
            if (engine.isPaused) return

            val data = getData(station)
            data.untilAttack -= amount * getRof()
            if (data.untilAttack <= 0f) {
                var strikesLeft = MathUtils.getRandomNumberInRange(TIMES_TO_STRIKE_MIN, TIMES_TO_STRIKE_MAX)
                if (engine.customData.get("\$SA_shroudedMantleCharging_${station.id}") == true) {
                    strikesLeft += MANTLE_CHARGE_STRIKE_INCREMENT
                }
                while (strikesLeft-- > 0) {
                    val ship = ((station.childModulesCopy + station).filter { it.isAlive }).randomOrNull() ?: return
                    val target: CombatEntityAPI? = findTarget(station, ship)
                    if (target != null) {
                        spawnExplosion(station, ship, target)
                    }
                }
                data.untilAttack = MIN_REFIRE_DELAY + Math.random().toFloat() * (MAX_REFIRE_DELAY - MIN_REFIRE_DELAY)
            }
        }

        fun spawnExplosion(station: ShipAPI, ship: ShipAPI, target: CombatEntityAPI) {
            val engine = Global.getCombatEngine()

            var angle = Misc.getAngleInDegrees(target.getLocation(), ship.getLocation())
            angle += 45f - 90f * Math.random().toFloat()
            var from = Misc.getUnitVectorAtDegreeAngle(angle)
            from.scale(10000f)

            val targetRadius = Misc.getTargetingRadius(from, target, false)
            val point = Misc.getUnitVector(target.getLocation(), from)
            point.scale(targetRadius * (0.8f + Math.random().toFloat() * 0.4f))
            Vector2f.add(target.getLocation(), point, point)

            var dist = Misc.getDistance(from, point)


            //float mult = getPowerMult(ship.getHullSize());
            val damage = ShroudedLensHullmod.getDamage(ship.getHullSize())

            if (FLUX_PER_DAMAGE > 0f) {
                val fluxCost = DAMAGE * FLUX_PER_DAMAGE
                //if (!ship.getFluxTracker().increaseFlux(fluxCost, false)) {
                if (ship.fluxTracker.maxFlux > 100f && !masterAugment.deductFlux(ship, fluxCost)) {
                    return
                }
            }

            val shroud = DwellerShroud.getShroudFor(ship)
            if (shroud != null) {
                angle = Misc.getAngleInDegrees(ship.getLocation(), point)
                from = Misc.getUnitVectorAtDegreeAngle(angle + 90f - 180f * Math.random().toFloat())
                from.scale(
                    (0.5f + Math.random()
                        .toFloat() * 0.25f) * shroud.getShroudParams().maxOffset * shroud.getShroudParams().overloadArcOffsetMult
                )
                Vector2f.add(ship.getLocation(), from, from)
            }

            var color = RiftLightningEffect.RIFT_LIGHTNING_COLOR

            if (shroud != null) {
                val shroudParams = shroud.getShroudParams()
                val params = EmpArcParams()
                params.segmentLengthMult = 4f
                params.glowSizeMult = 4f
                params.flickerRateMult = 0.5f + Math.random().toFloat() * 0.5f
                params.flickerRateMult *= 1.5f


                //Color fringe = shroudParams.overloadArcFringeColor;
                val fringe = color
                val core = Color.white

                val thickness = shroudParams.overloadArcThickness


                //Vector2f to = Misc.getPointAtRadius(from, 1f);
                angle = Misc.getAngleInDegrees(from, ship.getLocation())
                angle = angle + 90f * (Math.random().toFloat() - 0.5f)
                val dir = Misc.getUnitVectorAtDegreeAngle(angle)
                dist = shroudParams.maxOffset * shroud.getShroudParams().overloadArcOffsetMult
                dist = dist * 0.5f + dist * 0.5f * Math.random().toFloat()
                //dist *= 1.5f;
                dist *= 0.5f
                dir.scale(dist)
                val to = Vector2f.add(from, dir, Vector2f())

                val arc = engine.spawnEmpArcVisual(
                    from, ship, to, ship, thickness, fringe, core, params
                ) as EmpArcEntityAPI

                arc.setCoreWidthOverride(shroudParams.overloadArcCoreThickness)
                arc.setSingleFlickerMode(false)
                //arc.setRenderGlowAtStart(false);
            }


            //float explosionRadius = 40f + mult * 40f;
            val explosionRadius = RADIUS

            val spec = DamagingExplosionSpec(
                0.1f,  // duration
                explosionRadius,  // radius
                explosionRadius * 0.5f,  // coreRadius
                damage,  // maxDamage
                damage,  // / 2f, // minDamage - no damage dropoff with range
                CollisionClass.PROJECTILE_NO_FF,  // collisionClass
                CollisionClass.PROJECTILE_FIGHTER,  // collisionClassByFighter - using to flag it as from this effect
                3f,  // particleSizeMin
                3f,  // particleSizeRange
                0.5f,  // particleDuration
                0,  // particleCount
                Color(255, 255, 255, 0),  // particleColor
                Color(255, 100, 100, 0) // explosionColor
            )

            spec.setDamageType(DamageType.ENERGY)
            spec.setUseDetailedExplosion(false)
            spec.setSoundSetId("abyssal_glare_explosion")
            //spec.setSoundVolume(0.5f + 0.5f * mult);
            spec.setSoundVolume(0.33f)

            val explosion = engine.spawnDamagingExplosion(spec, ship, point, true)
            explosion.owner = if (target.owner == 0) 1 else 0

            //explosion.addDamagedAlready(target);
            //color = new Color(255,75,75,255);
            val baseSize = 7f

            val p = RiftCascadeMineExplosion.createStandardRiftParams(
                color, baseSize
            )
            //p.hitGlowSizeMult = 0.5f;
            p.noiseMult = 6f
            p.thickness = 25f
            p.fadeOut = 0.5f
            p.spawnHitGlowAt = 1f
            p.additiveBlend = true
            p.blackColor = Color.white
            p.underglow = null
            p.withNegativeParticles = false
            p.withHitGlow = false
            p.fadeIn = 0f

            //p.numRiftsToSpawn = 1;
            RiftCascadeMineExplosion.spawnStandardRift(explosion, p)


            // the "beam"
            val thickness = 30f
            //Color color = weapon.getSpec().getGlowColor();
            var coreColor = Color.white
            coreColor = Misc.zeroColor
            coreColor = color

            color = Color(255, 0, 30, 255)
            coreColor = Color(255, 10, 255, 255)

            color = DwellerShroud.SHROUD_GLOW_COLOR
            coreColor = color

            //coreColor = Color.white;
            val coreWidthMult = 1f


//		from = ship.getLocation();
//		if (shroud != null) {
//			angle = Misc.getAngleInDegrees(ship.getLocation(), target.getLocation());
//			from = Misc.getUnitVectorAtDegreeAngle(angle + 90f - 180f * (float) Math.random());
//			from.scale((0.5f + (float) Math.random() * 0.25f) * shroud.getShroudParams().maxOffset);
//			Vector2f.add(ship.getLocation(), from, from);
//		}
            val params = EmpArcParams()
            //params.segmentLengthMult = 10000f;
            params.segmentLengthMult = 4f


//		params.maxZigZagMult = 0f;
//		params.zigZagReductionFactor = 1f;
            params.maxZigZagMult = 0.25f
            //params.maxZigZagMult = 0f;
            params.zigZagReductionFactor = 1f


            //params.glowColorOverride = new Color(255,10,155,255);

            //params.zigZagReductionFactor = 0.25f;
            //params.maxZigZagMult = 0f;
            //params.flickerRateMult = 0.75f;
//		params.flickerRateMult = 1f;
//		params.flickerRateMult = 0.75f;
            params.flickerRateMult = 0.75f + 0.25f * Math.random().toFloat()

            params.fadeOutDist = 150f
            params.minFadeOutMult = 5f

            params.glowSizeMult = 0.5f


            //params.glowAlphaMult = 0.5f;
            //params.flamesOutMissiles = false;

//		params.movementDurOverride = 0.1f;
//		params.flickerRateMult = 0.5f;
//		params.glowSizeMult = 1f;
//		params.brightSpotFadeFraction = 0.1f;
//		params.brightSpotFullFraction = 0.9f;

//		params.maxZigZagMult = 1f;
//		params.zigZagReductionFactor = 0f;
//		params.flickerRateMult = 0.25f;
            val to = point
            val arc = engine.spawnEmpArcVisual(from, ship, to, explosion, thickness, color, coreColor, params)
            arc.setCoreWidthOverride(thickness * coreWidthMult)
            arc.setSingleFlickerMode()
            arc.setRenderGlowAtStart(false)
            if (shroud != null) {
                arc.setFadedOutAtStart(true)
            }
            arc.setWarping(0.2f)
        }

        fun findTarget(station: ShipAPI, ship: ShipAPI): CombatEntityAPI? {
            val maxRadius = shroudedMantle.ModuleType.getMaxDistOfModules(station) ?: return null
            val range = MAX_RANGE_ON_TOP_OF_RADIUS + maxRadius.second
            val from: Vector2f = ship.getLocation()

            val iter = Global.getCombatEngine().getAllObjectGrid().getCheckIterator(
                from,
                range * 2f, range * 2f
            )
            val owner: Int = ship.getOwner()
            var best: CombatEntityAPI? = null
            var minScore = Float.MAX_VALUE


            //boolean ignoreFlares = ship != null && ship.getMutableStats().getDynamic().getValue(Stats.PD_IGNORES_FLARES, 0) >= 1;
            //ignoreFlares |= weapon.hasAIHint(AIHints.IGNORES_FLARES);
            val ignoreFlares = false // doesn't care one way or another

            val picker = WeightedRandomPicker<CombatEntityAPI?>()

            val skipSelf = prob(95)
            while (iter.hasNext()) {
                val o = iter.next()
                if (o !is MissileAPI && o !is ShipAPI) continue
                val other = o
                if (other.getOwner() == owner) {
                    if (skipSelf) {
                        continue
                    }
                }

                if (other is ShipAPI) {
                    val otherShip = other
                    if (otherShip in station.childModulesCopy + station) continue
                    //if (otherShip.isHulk()) continue;
                    //if (!otherShip.isAlive()) continue;
                    if (otherShip.isPhased()) continue
                    if (!otherShip.isTargetable()) continue
                }

                if (other.getCollisionClass() == CollisionClass.NONE) continue

                if (ignoreFlares && other is MissileAPI) {
                    val missile = other
                    if (missile.isFlare()) continue
                }

                val targetRadius = Misc.getTargetingRadius(from, other, false)
                val shipRadius = Misc.getTargetingRadius(other.getLocation(), ship, false)
                val dist = Misc.getDistance(from, other.getLocation()) - targetRadius - shipRadius

                if (dist > range) continue

                val score = dist

                if (score < minScore) {
                    minScore = score
                    best = other
                }

                picker.add(other, 100f / max(100f, score))
            }


            //return best;
            return picker.pick()
        }
    }
}