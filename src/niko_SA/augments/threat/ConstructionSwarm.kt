package niko_SA.augments.threat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.CombatFleetManagerAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipAPI.HullSize
import com.fs.starfarer.api.combat.ShipVariantAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.combat.threat.ConstructionSwarmSystemScript
import com.fs.starfarer.api.impl.combat.threat.ConstructionSwarmSystemScript.SwarmConstructionData
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect
import com.fs.starfarer.api.impl.combat.threat.VoltaicDischargeOnFireEffect
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import lunalib.backend.ui.components.LunaUITextFieldWithSlider
import niko_SA.SA_mathUtils
import niko_SA.augments.core.AugmentMenuDialogueDelegate
import niko_SA.augments.shrouded.shroudedMantle
import niko_SA.augments.threat.KineticFragments.Level
import niko_SA.stringUtils
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import kotlin.math.min

class ConstructionSwarm: ThreatAugment() {

    companion object {
        const val HAZARD_RATING_INCREMENT = 0.1f

        val SIZES_TO_DELAY = mutableMapOf(
            Pair(HullSize.FRIGATE, 10f),
            Pair(HullSize.DESTROYER, 20f),
            Pair(HullSize.CRUISER, 30f),
            Pair(HullSize.CAPITAL_SHIP, 40f),
        )
    }

    enum class Level(val apMult: Float, val maxOverseers: Int, val shipsPerSpawn: Int, val constructionDelay: Float, val maxActiveFp: Float, val frontend: Int) {
        ONE(1f, 1, 1, 50f, 50f, 1),
        TWO(2f, 2, 2, 40f, 85f, 2),
        THREE(3f, 3, 2, 30f, 120f, 3);
    }

    var level: Level = Level.ONE

    override fun applyInCombat(station: ShipAPI) {
        Global.getCombatEngine().addPlugin(ConstructionSwarmAugmentScript(station, this,  level.maxActiveFp, level.maxOverseers, level.shipsPerSpawn, level.constructionDelay))
    }

    override fun apply() {
        super.apply()

        val stationIndustry = getStationIndustry() ?: return
        if (stationIndustry.isFunctional) {
            market?.hazard?.modifyFlat(id, HAZARD_RATING_INCREMENT, "${stationIndustry.currentName}: ${getName()}")
        }
    }

    override fun unapply() {
        super.unapply()

        market?.hazard?.unmodify(id)

    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Constructs a small garrison of threat ships during combat, albiet at extreme fragment cost. The " +
                "vessels harmlessly deconstitute after combat ends.",
            0f
        )

        tooltip.addPara(
            "The ships %s to the DP total of the defending side.",
            5f,
            Misc.getHighlightColor(),
            "do not contribute"
        )

        tooltip.addPara(
            "Increases hazard rating by %s.",
            5f,
            Misc.getNegativeHighlightColor(),
            stringUtils.toPercent(HAZARD_RATING_INCREMENT)
        )

        tooltip.addPara(
            "This augment is %s. It has %s modes that can be changed by a slider while applied.",
            5f,
            Misc.getHighlightColor(),
            "modular", "three"
        )

        tooltip.setBgAlpha(0.9f)

        val table = tooltip.beginTable(
            Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
            20f, true, true,
            *arrayOf<Any>("Mode", 70f, "Augment Cost", 130f, "Max FP", 80f)
        )
        val hl = Misc.getHighlightColor()
        useAPMult = false
        tooltip.addRow(
            Alignment.MID, hl, "One",
            Alignment.MID, hl, (getAugmentCost() * Level.ONE.apMult).toString(),
            Alignment.MID, hl, (Level.ONE.maxActiveFp).toString()
        )
        tooltip.addRow(
            Alignment.MID, hl, "Two",
            Alignment.MID, hl, (getAugmentCost() * Level.TWO.apMult).toString(),
            Alignment.MID, hl, (Level.TWO.maxActiveFp).toString()
        )
        tooltip.addRow(Alignment.MID, hl, "Three",
            Alignment.MID, hl, (getAugmentCost() * Level.THREE.apMult).toString(),
            Alignment.MID, hl, (Level.THREE.maxActiveFp).toString()
        )

        tooltip.addTable("", 0, 15f)
        useAPMult = true
    }

    var useAPMult = true
    override fun getAugmentCost(): Float {
        if (useAPMult) return super.getAugmentCost() * level.apMult
        return super.getAugmentCost()
    }

    override fun modifyAugmentMenu(tooltip: TooltipMakerAPI, panel: CustomPanelAPI?, buttonPanel: CustomPanelAPI?, delegate: AugmentMenuDialogueDelegate) {
        if (panel == null) return
        val slider = LunaUITextFieldWithSlider<Int>(level.frontend, Level.ONE.frontend.toFloat(), 3f, 100f, 50f, "SA_constructionFragSlider", "SA_constructionFragSlider", panel, tooltip)
        slider.onUpdate { events ->
            if (!events.isEmpty()) {
                if (slider.value != level.frontend) {
                    val foundLevel = Level.entries.first { it.frontend == slider.value }

                    useAPMult = false
                    if (getAPChangeInapplicableReason(getAugmentCost() * foundLevel.apMult) == null) {
                        level = foundLevel
                    } else {
                        slider.value = level.frontend
                    }
                    useAPMult = true
                    delegate.callback?.let { callback -> delegate.regenerateDialog(callback) }
                }
            }
        }
        slider.position?.inRMid(5f)
    }

    class ConstructionSwarmAugmentScript(
        val station: ShipAPI,
        val augment: ConstructionSwarm,
        val maxActiveFp: Float,
        val maxOverseers: Int,
        val maxShipsPerSpawn: Int,
        val constructionDelay: Float,
    ): BaseEveryFrameCombatPlugin() {
        val shipClearInterval = IntervalUtil(1f, 1.1f)
        val constructionInterval = IntervalUtil(constructionDelay, constructionDelay)

        val activeShips = HashSet<ShipAPI>()
        var lostShipDelay = 0f

        override fun init(engine: CombatEngineAPI?) {
            super.init(engine)

            constructionInterval.elapsed = constructionDelay * 0.9f // we get a build immediately
        }

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            val engine = Global.getCombatEngine()
            if (!station.isAlive) {
                engine.removePlugin(this)
                return
            }
            if (engine.isPaused) return

            shipClearInterval.advance(amount)
            if (shipClearInterval.intervalElapsed()) {
                for (ship in activeShips.toMutableSet()) {
                    if (!ship.isAlive) {
                        activeShips -= ship
                        lostShipDelay += SIZES_TO_DELAY[ship.hullSize] ?: 0f
                    }
                }

                for (ship in engine.ships) {
                    if (ship.isFighter) continue
                    if (ship.isStationModule) continue
                    if (ship.owner != station.owner) continue
                    if (!ship.hullSpec.hasTag(Tags.THREAT)) continue
                    if (ship.currentCR != 0.7f) continue
                    if (!ship.hasTag("ship_under_construction")) continue

                    activeShips += ship
                    ship.fleetMember.stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult("SA_threatShipDP", 0f)
                }
            }

            if (lostShipDelay > 0f) {
                lostShipDelay = (lostShipDelay - amount).coerceAtLeast(0f)
            } else {
                constructionInterval.advance(amount)
                if (constructionInterval.intervalElapsed()) {
                    attemptConstruction()
                }
            }
        }

        private fun attemptConstruction() {
            val shipsWeCanBuild = getTargetShips()
            if (shipsWeCanBuild.isNotEmpty()) {
                buildShips(shipsWeCanBuild)
            }
        }

        private fun buildShips(targets: MutableSet<ConstructionSwarmSystemScript.SwarmConstructableVariant>) {
            if (targets.isEmpty()) return

            val engine = Global.getCombatEngine()
            val manager: CombatFleetManagerAPI = engine.getFleetManager(station.owner)
            manager.isSuppressDeploymentMessages = true

            val stationSwarm = RoilingSwarmEffect.getSwarmFor(station)

            for (target in targets) {
                val variantId = target.variantId
                val variant = Global.getSettings().getVariant(variantId)
                if (variant == null) return
                val loc = getRandomPointForConstruction(variant)


                val fighter = manager.spawnShipOrWing(
                    SwarmLauncherEffect.CONSTRUCTION_SWARM_WING,
                    loc,
                    MathUtils.getRandomNumberInRange(0f, 360f),
                    0f,
                    null
                )
                if (station.owner == 0) {
                    fighter.isAlly = true
                }

                fighter.mutableStats.maxSpeed.modifyMult("construction_swarm", ConstructionSwarmSystemScript.CONSTRUCTION_SWARM_SPEED_MULT)

                fighter.isDoNotRender = true
                fighter.explosionScale = 0f
                fighter.hulkChanceOverride = 0f
                fighter.impactVolumeMult = SwarmLauncherEffect.IMPACT_VOLUME_MULT
                fighter.armorGrid.clearComponentMap() // no damage to weapons/engines

                val swarm = FragmentSwarmHullmod.createSwarmFor(fighter)
                swarm.params.flashFringeColor = VoltaicDischargeOnFireEffect.EMP_FRINGE_COLOR
                RoilingSwarmEffect.getFlockingMap().remove(swarm.params.flockingClass, swarm)
                swarm.params.flockingClass = FragmentSwarmHullmod.CONSTRUCTION_SWARM_FLOCKING_CLASS
                RoilingSwarmEffect.getFlockingMap().add(swarm.params.flockingClass, swarm)

                val dp = variant.hullSpec.suppliesToRecover

                val numFragments: Int = target.fragments
                var radiusMult = 1f
                var collisionMult = 2f
                var hpMult = 1f
                var travelTime = 3f

                if (variant.hullSize == HullSize.DESTROYER) {
                    radiusMult = 2f
                    collisionMult = 4f
                    hpMult = radiusMult
                    travelTime = 4f
                } else if (variant.hullSize == HullSize.CRUISER) {
                    radiusMult = 3.5f
                    collisionMult = 6f
                    hpMult = radiusMult
                    travelTime = 5f
                } else if (variant.hullSize == HullSize.CAPITAL_SHIP) {
                    radiusMult = 4f
                    collisionMult = 8f
                    hpMult = radiusMult
                    travelTime = 6f
                }

                for (s in fighter.exactBounds.origSegments) {
                    s.p1.scale(collisionMult)
                    s.p2.scale(collisionMult)
                    s.set(s.p1.x, s.p1.y, s.p2.x, s.p2.y)
                }
                fighter.collisionRadius = fighter.collisionRadius * collisionMult

                fighter.maxHitpoints = fighter.maxHitpoints * hpMult
                fighter.hitpoints = fighter.hitpoints * hpMult

                swarm.params.maxOffset *= radiusMult


                //		swarm.params.initialMembers *= numMult;
//		swarm.params.baseMembersToMaintain = swarm.params.initialMembers;
//		requiredFragments = swarm.params.initialMembers;
                swarm.params.initialMembers = numFragments
                swarm.params.baseMembersToMaintain = numFragments

                val overseer = variant.hullSpec.hasTag(Tags.THREAT_OVERSEER)

                val data = SwarmConstructionData()
                data.variantId = variantId
                data.constructionTime =
                    ConstructionSwarmSystemScript.BASE_CONSTRUCTION_TIME + dp * ConstructionSwarmSystemScript.CONSTRUCTION_TIME_DP_MULT
                if (overseer) {
                    data.constructionTime += ConstructionSwarmSystemScript.CONSTRUCTION_TIME_OVERSEER_EXTRA
                }
                data.preConstructionTravelTime = travelTime * 1f

                swarm.custom1 = data

                val transfer = min(numFragments.toDouble(), stationSwarm.numActiveMembers.toDouble()).toInt()
                if (transfer > 0) {
                    //loc = Vector2f(takeoffVel)
                    //loc.scale(0.5f)
                    //Vector2f.add(loc, fighter.location, loc)
                    stationSwarm.transferMembersTo(swarm, transfer, loc, 100f)
                }

                val add = numFragments - transfer
                if (add > 0) {
                    swarm.addMembers(add)
                }
            }

            manager.isSuppressDeploymentMessages = false
        }

        private fun getRandomPointForConstruction(variant: ShipVariantAPI): Vector2f {
            //val swarm = RoilingSwarmEffect.getSwarmFor(station) ?: return Vector2f(0f, 0f)
            val maxModule = shroudedMantle.ModuleType.getMaxDistOfModules(station) ?: return Misc.ZERO
            val max = (maxModule.second + maxModule.first.collisionRadius) + 400f + variant.hullSpec.collisionRadius
            val isPlayerAlly = station.owner == 0

            var facing = 0f
            if (isPlayerAlly) {
                facing = Misc.normalizeAngle(270f + (MathUtils.getRandomNumberInRange(-45f, 45f)))
            } else {
                facing = Misc.normalizeAngle(90f + (MathUtils.getRandomNumberInRange(-45f, 45f)))
            }

            return MathUtils.getPointOnCircumference(station.location, max, facing)
        }

        private fun getTargetShips(): MutableSet<ConstructionSwarmSystemScript.SwarmConstructableVariant> {
            val variants = HashSet<ConstructionSwarmSystemScript.SwarmConstructableVariant>()

            val currFP = getUsedFP()
            var FPremaining = maxActiveFp - currFP

            var picksLeft = maxShipsPerSpawn
            ConstructionSwarmSystemScript.init()
            val constructableCopy = ConstructionSwarmSystemScript.CONSTRUCTABLE.toMutableList()
            val swarm = RoilingSwarmEffect.getSwarmFor(station) ?: return variants
            if (getNumOverseers() < 1) {
                val targetVariant = constructableCopy.first { it.type == ConstructionSwarmSystemScript.SwarmConstructableType.OVERSEER }
                if (targetVariant.dp <= FPremaining && targetVariant.fragments <= swarm.members.size) {
                    variants += targetVariant
                    FPremaining -= targetVariant.dp
                }
            }

            while (picksLeft > 0 && FPremaining > 0 && constructableCopy.isNotEmpty()) {
                val randPick = constructableCopy.random()
                if (randPick.type == ConstructionSwarmSystemScript.SwarmConstructableType.OVERSEER && getNumOverseers() >= maxOverseers) {
                    constructableCopy -= randPick
                    continue
                }

                if (randPick.dp >= FPremaining) {
                    constructableCopy -= randPick
                    continue
                }

                if (randPick.fragments > swarm.members.size) {
                    constructableCopy -= randPick
                    continue
                }

                if (randPick.type == ConstructionSwarmSystemScript.SwarmConstructableType.HIVE && SA_mathUtils.prob(40)) {
                    constructableCopy -= randPick
                    continue
                }

                variants += randPick

                picksLeft--
            }

            return variants
        }

        fun getNumOverseers(): Int {
            var num = 0
            for (ship in activeShips) {
                if (ship.hullSpec.hasTag(Tags.THREAT_OVERSEER)) num++
            }
            return num
        }

        private fun getUsedFP(): Float {
            var used = 0f
            for (ship in activeShips) {
                ship.fleetMember.stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).unmodify("SA_threatShipDP")
                val FP = ship.fleetMember.deploymentPointsCost
                ship.fleetMember.stats.dynamic.getMod(Stats.DEPLOYMENT_POINTS_MOD).modifyMult("SA_threatShipDP", 0f)
                used += FP
            }
            return used
        }
    }
}