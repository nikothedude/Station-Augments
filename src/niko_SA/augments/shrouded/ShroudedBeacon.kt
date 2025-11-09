package niko_SA.augments.shrouded

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CollisionClass
import com.fs.starfarer.api.combat.DamageType
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.combat.RiftLanceEffect
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud
import com.fs.starfarer.api.impl.combat.dweller.DwellerShroud.SHROUD_COLOR
import com.fs.starfarer.api.impl.hullmods.ShardSpawner
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import niko_SA.SA_mathUtils
import niko_SA.SA_miscUtils
import niko_SA.SA_miscUtils.getFurthestModule
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lazywizard.lazylib.combat.AIUtils
import org.lwjgl.util.vector.Vector2f

class ShroudedBeacon: ShroudedAugment() {

    companion object {
        const val MAX_DP = 120f
        const val SUMMON_DELAY_MIN = 2f
        const val SUMMON_DELAY_MAX = 45f
        const val FADE_IN_TIME = 6f

        const val TARGET_FRIENDLY_CHANCE = 30f
        // if we are targetting a friend
        const val TARGET_STATION_CHANCE = 25f

        val VARIANTS_TO_WEIGHT = mutableMapOf(
            Pair("shrouded_tendril_Roiling", 100f),
            Pair("shrouded_maelstrom_Menacing", 50f),
            Pair("shrouded_eye_Darkened", 20f),
            Pair("shrouded_maw_Ravenous", 10f),
        )
    }

    override val incompatibleAugments: MutableSet<String> = mutableSetOf("SA_shroudedLens")

    override fun applyInCombat(station: ShipAPI) {
        super.applyInCombat(station)

        Global.getCombatEngine().addPlugin(
            ShroudedBeaconCombatPlugin(station)
        )
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "\"A beacon, sufficiently bright, will attract travellers in even the brightest of days. Those seeking shelter yes, but... also those " +
            "who wish to snuff it out.\"",
            5f
        ).color = Misc.getGrayColor()

        tooltip.addPara(
            "Uses the %s as a beacon, rather than a focus - transmitting a signal in an unknowable dimension towards unknowable foes. \"Come hither\", it demands.",
            5f,
            Misc.getHighlightColor(),
            "lens"
        )
        tooltip.addPara(
            "Attracts %s \"vessels\" into the battlespace. These entities will manifest near ships, with a %s towards manifesting near an enemy - however, " +
            "the lens is unpredictable, and may manifest a entity in a random location - or even near an %s.",
            5f,
            Misc.getHighlightColor(),
            "Shrouded Dweller", "heavy bias", "ally"
        ).setHighlightColors(
            Global.getSector().getFaction(Factions.DWELLER).baseUIColor,
            Misc.getHighlightColor(),
            Misc.getNegativeHighlightColor()
        )
        tooltip.addPara(
            "The vessels are %s to the nearest ship, and will maintain this hostility even once their target perishes.",
            5f,
            Misc.getHighlightColor(),
            "immediately hostile"
        )
        tooltip.addPara(
            "If the station is destroyed, or combat ends, the beacon is extinguished - which will, hopefully, immediately banish " +
            "the entities.",
            5f
        )

        tooltip.addPara(
            "Deploys up to %s worth of DP before pausing. This budget is shared by both sides.",
            5f,
            Misc.getHighlightColor(),
            "${MAX_DP.toInt()}"
        )
    }

    class ShroudedBeaconCombatPlugin(
        val station: ShipAPI
    ): BaseEveryFrameCombatPlugin() {
        val shipClearInterval = IntervalUtil(1f, 1.1f)
        val summonInterval = IntervalUtil(SUMMON_DELAY_MIN, SUMMON_DELAY_MAX)
        val activeShips = HashSet<ShipAPI>()

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            val engine = Global.getCombatEngine()
            if (!station.isAlive) {
                nuke()
                engine.removePlugin(this)
                return
            }
            if (engine.isPaused) return

            shipClearInterval.advance(amount)
            if (shipClearInterval.intervalElapsed()) {
                for (ship in activeShips.toMutableSet()) {
                    if (!ship.isAlive) {
                        activeShips -= ship
                    }
                }
            }

            summonInterval.advance(amount)
            if (summonInterval.intervalElapsed()) {
                val available = getAvailableSpawns()
                if (available.isEmpty()) return
                val picker = WeightedRandomPicker<ShipVariantAPI>()
                available.forEach { picker.add(it.key, it.value) }

                val picked = picker.pick()

                val targetFriend = SA_mathUtils.prob(TARGET_FRIENDLY_CHANCE)
                manifest(targetFriend, picked)
            }
        }

        private fun getUsedFP(): Float {
            var used = 0f
            for (ship in activeShips) {
                ship.fleetMember.stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify("SA_dwellerShipDP")
                val FP = ship.fleetMember.deploymentPointsCost
                ship.fleetMember.stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult("SA_dwellerShipDP", 0f)
                used += FP
            }
            return used
        }

        fun getAvailableSpawns(): MutableMap<ShipVariantAPI, Float> {
            val variants = HashMap<ShipVariantAPI, Float>()
            val used = getUsedFP()
            val remainder = MAX_DP - used

            for (entry in VARIANTS_TO_WEIGHT) {
                val variant = Global.getSettings().getVariant(entry.key)
                val DP = variant.hullSpec.suppliesToRecover
                if (DP > remainder) continue

                variants[variant] = VARIANTS_TO_WEIGHT[entry.key]!!
            }

            return variants
        }

        fun manifest(targetFriendly: Boolean, picked: ShipVariantAPI) {
            val owner: Int
            if (targetFriendly) {
                if (station.owner == 1) owner = 0 else owner = 1
            } else owner = station.owner

            val engine = Global.getCombatEngine()
            val fleetManager = engine.getFleetManager(owner)
            val targetStation = (targetFriendly && SA_mathUtils.prob(TARGET_STATION_CHANCE))
            val spawnLoc: Vector2f
            val facing: Float

            if (targetStation) {
                val furthestModule = station.getFurthestModule()
                val colRadius = MathUtils.getDistance(station.location, furthestModule.location) + furthestModule.collisionRadius
                spawnLoc = MathUtils.getPointOnCircumference(station.location, colRadius * 3f, MathUtils.getRandomNumberInRange(0f, 360f))
                facing = VectorUtils.getAngle(spawnLoc, station.location)
            } else {

                val enemies = engine.ships.filter { it != station && it !in station.childModulesCopy && it.owner != owner && !it.isFighter && it.isAlive && !it.isHulk }
                if (enemies.isEmpty()) return
                val target = enemies.minBy { MathUtils.getDistance(station, it) }
                val colRadius = target.collisionRadius
                spawnLoc = MathUtils.getPointOnCircumference(target.location, colRadius * 3f, MathUtils.getRandomNumberInRange(0f, 360f))
                facing = VectorUtils.getAngle(spawnLoc, target.location)
            }

            val oldSetting = fleetManager.isSuppressDeploymentMessages
            fleetManager.isSuppressDeploymentMessages = true
            // spawn
            val ship = fleetManager.spawnShipOrWing(
                picked.hullVariantId,
                spawnLoc,
                facing
            )
            ship.fleetMember.stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult("SA_dwellerShipDP", 0f)
            ship.name = "Horror"
            if (owner == 0) ship.isAlly = true
            Global.getSoundPlayer().playSound(
                "assaying_rift_fire",
                1f,
                2f,
                spawnLoc,
                Misc.ZERO
            )
            fleetManager.isSuppressDeploymentMessages = oldSetting

            activeShips += ship
            Global.getCombatEngine().addPlugin(DwellerFadeInPlugin(ship, FADE_IN_TIME, facing))
        }

        fun nuke() {
            for (ship in activeShips) {
                val stats = ship.mutableStats
                stats.hullDamageTakenMult.unmodify()
                stats.armorDamageTakenMult.unmodify()
                ship.hitpoints = 1f
                Global.getCombatEngine().applyDamage(
                    ship,
                    ship.location,
                    9999999f,
                    DamageType.ENERGY,
                    0f,
                    true,
                    false,
                    null
                )
            }
            activeShips.clear()
        }
    }

    class DwellerFadeInPlugin(var ship: ShipAPI, var fadeInTime: Float, var angle: Float) :
        BaseEveryFrameCombatPlugin() {
        var elapsed: Float = 0f
        var interval: IntervalUtil = IntervalUtil(0.075f, 0.125f)

        override fun advance(amount: Float, events: List<InputEventAPI>) {
            if (Global.getCombatEngine().isPaused) return

            elapsed += amount

            val engine = Global.getCombatEngine()

            var progress = (elapsed) / fadeInTime
            if (progress > 1f) progress = 1f

            //ship.alphaMult = progress
            //ship.extraAlphaMult = progress
            val shroud = DwellerShroud.getShroudFor(ship)
            shroud.shroudParams.alphaMult = progress
            ship.alphaMult = progress

            if (progress < 0.5f) {
                ship.blockCommandForOneFrame(ShipCommand.ACCELERATE)
                ship.blockCommandForOneFrame(ShipCommand.TURN_LEFT)
                ship.blockCommandForOneFrame(ShipCommand.TURN_RIGHT)
                ship.blockCommandForOneFrame(ShipCommand.STRAFE_LEFT)
                ship.blockCommandForOneFrame(ShipCommand.STRAFE_RIGHT)
            }

            ship.blockCommandForOneFrame(ShipCommand.USE_SYSTEM)
            ship.blockCommandForOneFrame(ShipCommand.TOGGLE_SHIELD_OR_PHASE_CLOAK)
            ship.blockCommandForOneFrame(ShipCommand.FIRE)
            ship.blockCommandForOneFrame(ShipCommand.PULL_BACK_FIGHTERS)
            ship.blockCommandForOneFrame(ShipCommand.VENT_FLUX)
            ship.isHoldFireOneFrame = true
            ship.isHoldFire = true

            ship.collisionClass = CollisionClass.NONE
            ship.mutableStats.hullDamageTakenMult.modifyMult("ShardSpawnerInvuln", 0f)
            if (progress < 0.5f) {
                ship.velocity.set(Vector2f())
            } else if (progress > 0.75f) {
                ship.collisionClass = CollisionClass.SHIP
                ship.mutableStats.hullDamageTakenMult.unmodifyMult("ShardSpawnerInvuln")
            }


            //					Vector2f dir = Misc.getUnitVectorAtDegreeAngle(Misc.getAngleInDegrees(source.getLocation(), ship.getLocation()));
//					dir.scale(amount * 50f * progress);
//					Vector2f.add(ship.getLocation(), dir, ship.getLocation());
            var jitterLevel = progress
            if (jitterLevel < 0.5f) {
                jitterLevel *= 2f
            } else {
                jitterLevel = (1f - jitterLevel) * 2f
            }

            val jitterRange = 1f - progress
            val maxRangeBonus = 50f
            val jitterRangeBonus = jitterRange * maxRangeBonus
            var c = ShardSpawner.JITTER_COLOR

            ship.setJitter(this, c, jitterLevel, 25, 0f, jitterRangeBonus)

            interval.advance(amount)
            if (interval.intervalElapsed() && progress < 0.8f) {
                c = RiftLanceEffect.getColorForDarkening(SHROUD_COLOR)
                val baseDuration = 2f
                val vel = Vector2f(ship.velocity)
                val size = ship.collisionRadius * 0.35f
                for (i in 0..2) {
                    var point = Vector2f(ship.location)
                    point = Misc.getPointWithinRadiusUniform(point, ship.collisionRadius * 0.5f, Misc.random)
                    var dur = baseDuration + baseDuration * Math.random().toFloat()
                    val nSize = size
                    val pt = Misc.getPointWithinRadius(point, nSize * 0.5f)
                    val v = Misc.getUnitVectorAtDegreeAngle(Math.random().toFloat() * 360f)
                    v.scale(nSize + nSize * Math.random().toFloat() * 0.5f)
                    v.scale(0.2f)
                    Vector2f.add(vel, v, v)

                    val maxSpeed = nSize * 1.5f * 0.2f
                    val minSpeed = nSize * 1f * 0.2f
                    val overMin = v.length() - minSpeed
                    if (overMin > 0) {
                        var durMult = 1f - overMin / (maxSpeed - minSpeed)
                        if (durMult < 0.1f) durMult = 0.1f
                        dur *= 0.5f + 0.5f * durMult
                    }
                    engine.addNegativeNebulaParticle(
                        pt, v, nSize * 1f, 2f,
                        0.5f / dur, 0f, dur, c
                    )
                }
            }

            if (elapsed > fadeInTime) {
                ship.alphaMult = 1f
                ship.isHoldFire = false
                ship.collisionClass = CollisionClass.SHIP
                ship.mutableStats.hullDamageTakenMult.unmodifyMult("ShardSpawnerInvuln")
                engine.removePlugin(this)
            }
        }
    }
}