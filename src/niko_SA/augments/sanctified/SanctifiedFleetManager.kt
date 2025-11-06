package niko_SA.augments.sanctified

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.FleetAssignment
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3
import com.fs.starfarer.api.impl.campaign.fleets.SourceBasedFleetManager
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.intel.events.LuddicChurchHostileActivityFactor
import com.fs.starfarer.api.impl.campaign.intel.group.FGAction
import com.fs.starfarer.api.impl.campaign.intel.group.FGBlockadeAction
import com.fs.starfarer.api.impl.campaign.intel.group.FGBlockadePlanetAction
import com.fs.starfarer.api.impl.campaign.intel.group.FGRaidAction
import com.fs.starfarer.api.impl.campaign.intel.group.GenericRaidFGI.PAYLOAD_ACTION
import com.fs.starfarer.api.impl.campaign.intel.group.KnightsOfLuddTakeoverExpedition
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import lunalib.lunaExtensions.getMarketsCopy
import niko_SA.SA_baseNikoScript
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils
import kotlin.math.ceil
import kotlin.math.roundToInt

class SanctifiedFleetManager(source: SectorEntityToken, thresholdLY: Float, minFleets: Int, maxFleets: Int,
                             respawnDelay: Float, val augment: sanctifiedStation
) : SourceBasedFleetManager(source,
    thresholdLY, minFleets, maxFleets, respawnDelay
) {

    companion object {
        const val FP_MIN = 50f
        const val FP_MAX = 60f

        fun churchExpeditionThreatening(market: MarketAPI): Boolean {
            if (!market.isPlayerOwned) return false
            val intel = Global.getSector().intelManager.getFirstIntel(KnightsOfLuddTakeoverExpedition::class.java) as? KnightsOfLuddTakeoverExpedition ?: return false

            return intel.currentAction is FGBlockadeAction || intel.currentAction is FGBlockadePlanetAction || intel.currentAction is FGRaidAction
        }
    }

    init {
        destroyed = maxFleets.toFloat() // we dont want them all to spawn instantly bro
    }

    val checkInterval = IntervalUtil(0.3f, 0.4f)

    override fun advance(amount: Float) {
        super.advance(amount)
        if (isDone || augment.market == null || augment.market?.isPlayerOwned != true) return
        val days = Misc.getDays(amount)
        checkInterval.advance(days)
        if (checkInterval.intervalElapsed()) {

            val player = Global.getSector().playerFleet
            val distFromSource = Misc.getDistanceLY(player.locationInHyperspace, sourceLocation)
            var f = 0f
            if (distFromSource < thresholdLY) {
                f = (thresholdLY - distFromSource) / (thresholdLY * 0.1f)
                if (f > 1) f = 1f
            }

            var currMax: Int = minFleets + ((maxFleets - minFleets) * f).roundToInt()
            currMax = (currMax - ceil(destroyed.toDouble())).toInt()

            if (source == null) {
                currMax = 0
            }

            if (currMax < fleets.size) {
                for (fleet in fleets.toList()) {
                    despawnFleet(fleet)
                    fleets.remove(fleet)
                    if (currMax >= fleets.size) break
                }
            }
        }
    }

    override fun spawnFleet(): CampaignFleetAPI? {
        augment.market?.let { if (churchExpeditionThreatening(it)) return null }
        val market = augment.market ?: return null
        val fp = MathUtils.getRandomNumberInRange(FP_MIN, FP_MAX)
        val params = FleetParamsV3(
            market,
            market.locationInHyperspace,
            Factions.LUDDIC_CHURCH,
            null,
            FleetTypes.SACRED_PROTECTORS,
            fp,
            0f,
            0f,
            0f,
            0f,
            0f,
            0f
        )
        params.ignoreMarketFleetSizeMult = true
        val entity = augment.getStationCampaignEntity() ?: return null
        val fleet = FleetFactoryV3.createFleet(params)
        fleet.setFaction(market.faction.id, false)
        fleet.isNoFactionInName = true
        fleet.name = "${entity.name} Vigil"

        fleet.clearAssignments()
        fleet.addAssignment(
            FleetAssignment.ORBIT_PASSIVE,
            entity,
            MathUtils.getRandomNumberInRange(30f, 40f),
            "in sacred vigil",
            null
        )
        fleet.addAssignment(
            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
            entity,
            Float.MAX_VALUE
        )
        DefenseScript(fleet, augment).start()

        fleet.memoryWithoutUpdate[MemFlags.MEMORY_KEY_FLEET_DO_NOT_GET_SIDETRACKED] = true
        fleet.memoryWithoutUpdate["\$SA_vigilFleet"] = true

        entity.containingLocation.addEntity(fleet)
        fleet.setLocation(entity.location.x, entity.location.y)
        fleet.facing = MathUtils.getRandomNumberInRange(0f, 360f)

        return fleet
    }

    fun delete() {
        Global.getSector().removeScript(this)
        for (fleet in fleets) {
            despawnFleet(fleet)
        }
        isDone = true
    }

    private fun despawnFleet(fleet: CampaignFleetAPI) {
        val viableMrkt = augment.market ?: fleet.faction.getMarketsCopy().randomOrNull() ?: Global.getSector().economy.marketsCopy.random()

        fleet.clearAssignments()
        fleet.addAssignmentAtStart(
            FleetAssignment.GO_TO_LOCATION_AND_DESPAWN,
            viableMrkt.primaryEntity,
            Float.MAX_VALUE,
            null
        )
    }

    fun setFleets(num: Int) {
        maxFleets = num
        minFleets = num - 1
    }

    fun updateFaction(faction: FactionAPI?) {
        if (faction == null) return

        for (fleet in fleets) {
            fleet.setFaction(faction.id, false)
        }
    }

    class DefenseScript(val fleet: CampaignFleetAPI, val augment: stationAttachment): SA_baseNikoScript() {
        override fun isDone(): Boolean {
            return false
        }

        override fun startImpl() {
            fleet.addScript(this)
        }

        override fun stopImpl() {
            fleet.removeScript(this)
        }

        override fun runWhilePaused(): Boolean {
            return false
        }

        override fun advance(amount: Float) {
            if (!augment.applied) {
                delete()
                return
            }

            if (fleet.battle != null) return
            val target = augment.getStationFleet() ?: return
            if (target.battle != null && fleet.assignmentsCopy.firstOrNull()?.assignment != FleetAssignment.INTERCEPT) {
                val canJoinBattle = target.battle.canJoin(fleet)

                if (canJoinBattle) {
                    val targetFleet = target.battle.getOtherSideFor(target).firstOrNull() ?: return
                    fleet.addAssignmentAtStart(
                        FleetAssignment.INTERCEPT,
                        target,
                        0.2f,
                        "assisting in battle",
                        null
                    )
                }
            }
        }

    }
}