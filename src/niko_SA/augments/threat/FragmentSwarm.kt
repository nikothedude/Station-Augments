package niko_SA.augments.threat

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.DamagingProjectileAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.combat.threat.BaseFragmentMissileEffect
import com.fs.starfarer.api.impl.combat.threat.FragmentSwarmHullmod
import com.fs.starfarer.api.impl.combat.threat.FragmentWeapon
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getStationAugments
import niko_SA.augments.core.stationAttachment
import niko_SA.augments.shrouded.shroudedMantle
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

class FragmentSwarm: stationAttachment() {

    companion object {
        fun createSwarmFor(station: ShipAPI): RoilingSwarmEffect {
            val swarm = RoilingSwarmEffect.getSwarmFor(station)
            if (swarm != null) return swarm

            val baseSwarm = FragmentSwarmHullmod.createSwarmFor(station)
            val maxModule = shroudedMantle.ModuleType.getMaxDistOfModules(station) ?: throw RuntimeException("fuck it we just crash here")
            val newOffset = getOffsetFromDistModule(maxModule)
            baseSwarm.params.maxOffset = newOffset
            baseSwarm.params.baseMembersToMaintain = MEMBERS_TO_MAINTAIN
            baseSwarm.params.memberRespawnRate = RESPAWN_RATE

            baseSwarm.params.maxNumMembersToAlwaysRemoveAbove = (baseSwarm.params.baseMembersToMaintain * 1.5f).toInt()
            baseSwarm.params.initialMembers = baseSwarm.params.baseMembersToMaintain

            baseSwarm.params.removeMembersAboveMaintainLevel = false

            return baseSwarm
        }

        fun createDroneSwarm(drone: ShipAPI): RoilingSwarmEffect {
            val swarm = RoilingSwarmEffect.getSwarmFor(drone)
            if (swarm != null) return swarm

            val baseSwarm = FragmentSwarmHullmod.createSwarmFor(drone)
            //baseSwarm.params.baseMembersToMaintain = DRONE_MEMBERS
            //baseSwarm.params.memberRespawnRate = RESPAWN_RATE

            //baseSwarm.params.maxNumMembersToAlwaysRemoveAbove = (baseSwarm.params.baseMembersToMaintain * 1.5f).toInt()
            //baseSwarm.params.initialMembers = baseSwarm.params.baseMembersToMaintain

            //baseSwarm.params.removeMembersAboveMaintainLevel = false

            return baseSwarm
        }

        fun getOffsetFromDistModule(maxModule: Pair<ShipAPI, Float>): Float {
            return ceil((maxModule.second * 0.82f) + (maxModule.first.collisionRadius))
        }

        const val MEMBERS_TO_MAINTAIN = 225
        const val RESPAWN_RATE = 6f
    }

    override fun applyInCombat(station: ShipAPI) {
        applyOverlays(station)
        applySwarm(station)

        Global.getCombatEngine().addPlugin(FragmentSwarmAugmentPlugin(
            station,
            this
        ))
    }

    override fun getUnavailableReason(): String? {
        if (market != null) {
            for (augment in market!!.getStationAugments()) {
                val spec = augment.getSpec()
                if (spec.knowledgeTags.contains(Tags.SHROUDED)) return "Incompatible with shrouded augments"
            }
        }
        return super.getUnavailableReason()
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "A swarm of Threat fragments roils around the ship, providing a regenerating supply of fragments for " +
            "fragment-based augments.", 0f
        )

        tooltip.addPara(
            "The base number of fragments is %s, and replacement fragments are launched %s times a second.",
            5f,
            Misc.getHighlightColor(),
            "$MEMBERS_TO_MAINTAIN", "${RESPAWN_RATE.toInt()}"
        )

        tooltip.addPara(
            "This augment by itself does not provide autoresolve strength. However, fragment-based augments have a noticably " +
            "higher ratio of AP-to-strength, making threat augments a worthwhile investment.",
            5f
        ).color = Misc.getGrayColor()
    }

    private fun applySwarm(station: ShipAPI) {
        createSwarmFor(station)
        for (module in station.childModulesCopy) { //TODO: THIS MIGHT BE A HORRIBLE IDEA LOL
            RoilingSwarmEffect.getShipMap()[module] = RoilingSwarmEffect.getSwarmFor(station)
        }
    }

    private fun applyOverlays(station: ShipAPI) {
        for (module in (station.childModulesCopy + station)) {
            applyOverlay(module)
        }
    }

    fun applyOverlay(module: ShipAPI) {
        module.setExtraOverlay(Global.getSettings().getSpriteName("misc", "fragment_swarm"))
        module.isExtraOverlayMatchHullColor = false
        module.extraOverlayShadowOpacity = 1f
    }

    class FragmentSwarmAugmentPlugin(
        val station: ShipAPI,
        val augment: FragmentSwarm
    ): BaseEveryFrameCombatPlugin() {

        val checkInterval = IntervalUtil(0.2f, 0.21f)

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            val engine = Global.getCombatEngine()

            if (!station.isAlive) return
            if (engine.isPaused) return

            var swarm = RoilingSwarmEffect.getSwarmFor(station)
            if (swarm == null) {
                swarm = createSwarmFor(station)
            }

            if (station.isFighter) return

            val playership = Global.getCurrentState() == GameState.COMBAT && Global.getCombatEngine() != null && Global.getCombatEngine().playerShip in (station.childModulesCopy + station)

            checkInterval.advance(amount)
            val params = swarm.params

            if (checkInterval.intervalElapsed()) {
                val maxModule = shroudedMantle.ModuleType.getMaxDistOfModules(station)
                if (maxModule != null) {
                    val newOffset = getOffsetFromDistModule(maxModule)
                    if (newOffset != params.maxOffset) {
                        params.maxOffset = (maxModule.second * 0.7f)
                        for (member in swarm.members) {
                            val dist = member.offset
                            if (abs(dist.x) > newOffset || abs(dist.y) > newOffset) {
                                member.rollOffset(
                                    swarm.params,
                                    station
                                )
                            }
                        }
                    }
                }

                if (engine.customData["SA_droneSwarmEnabled_${station.id}"] == true && station.deployedDrones != null && station.deployedDrones.isNotEmpty()) {
                    for (drone in station.deployedDrones) {
                        val droneSwarm = RoilingSwarmEffect.getSwarmFor(drone)
                        if (droneSwarm != null) continue
                        createDroneSwarm(drone)
                        augment.applyOverlay(drone)
                    }
                }
            }

            params.baseMembersToMaintain = station.mutableStats.dynamic.getValue(Stats.FRAGMENT_SWARM_SIZE_MOD, MEMBERS_TO_MAINTAIN.toFloat()).toInt()
            params.memberRespawnRate = RESPAWN_RATE * station.mutableStats.dynamic.getValue(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT)


//		if (station.getHullSpec().getHullId().equals(ThreatHullmod.HIVE_UNIT)) {
//			params.baseMembersToMaintain = SwarmLauncherEffect.FRAGMENT_NUM.get(SwarmLauncherEffect.ATTACK_SWARM_WING);
//			params.baseMembersToMaintain *= 8;
//			params.memberRespawnRate = 15 * station.getMutableStats().getDynamic().getValue(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT);
//		}
            params.maxNumMembersToAlwaysRemoveAbove = (params.baseMembersToMaintain * 1.5f).toInt()
            params.initialMembers = params.baseMembersToMaintain

            if (playership) {
                val active = swarm.numActiveMembers

                val thePlayerShip = Global.getCombatEngine().playerShip

                var maxRequired = 0
                for (w in thePlayerShip.allWeapons) {
                    if (w.effectPlugin is FragmentWeapon) {
                        val fw = w.effectPlugin as FragmentWeapon
                        maxRequired = max(maxRequired, fw.numFragmentsToFire)
                    }
                }

                val debuff = active < maxRequired
                Global.getCombatEngine().maintainStatusForPlayerShip(
                    FragmentSwarmHullmod.STATUS_KEY1,
                    Global.getSettings().getSpriteName("ui", "icon_tactical_fragment_swarm"),
                    augment.getSpec().name,
                    "FRAGMENTS: $active",
                    debuff
                )
            }
        }
    }
}