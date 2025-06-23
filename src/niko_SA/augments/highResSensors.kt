package niko_SA.augments

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_fleetUtils.getRepLevelForArrayBonus
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils

class highResSensors() : stationAttachment(), EveryFrameScript {
    val UUID = Misc.genUID()

    companion object {
        const val SIGHT_MULT = 4f

        const val MAX_SENSOR_BONUS = 1000f
        const val DIST_FOR_MAX_EFFECT = 400f
        const val DIST_FOR_MIN_EFFECT = 2600f
    }

    override fun applyInCombat(station: ShipAPI) {
        station.mutableStats.sightRadiusMod.modifyMult(id, 4f)
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    val interval = IntervalUtil(0.05f, 0.08f) // days

    override fun apply() {
        super.apply()
        market?.primaryEntity?.addScript(this)
    }

    override fun unapply() {
        super.unapply()
        market?.primaryEntity?.removeScript(this)
    }

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (!interval.intervalElapsed()) return

        val stationEntity = getStationCampaignEntity() ?: return
        val ourFaction = market?.faction ?: return
        for (fleet in stationEntity.containingLocation.fleets) {
            val repLevelNeeded = fleet.getRepLevelForArrayBonus()
            if (ourFaction.getRelationshipLevel(fleet.faction) < repLevelNeeded) continue

            var effectLevel = getStationCampaignEntity()?.let { getPercentEffectiveness(fleet, it) } ?: return

            fleet.stats.removeTemporaryMod(UUID) // maybe this will work?
            /*val tempMod = niko_MPC_reflectionUtils.get("tempMods", fleet.stats) as? Map<String, MutableFleetStats.TemporaryStatMod> ?: return
            val mod = tempMod[UUID]
            val stat = mod?.let { niko_MPC_reflectionUtils.get("stat", it) } as? StatBonus ?: return
            if (stat != fleet.stats.sensorRangeMod) {
                niko_MPC_debugUtils.log.error("FOUND INCORRECT STATMOD! $stat")
                return
            } else {
            }*/

            fleet.stats.addTemporaryModFlat(0.2f, UUID, "${market!!.name} ${getName()}", MAX_SENSOR_BONUS * effectLevel, fleet.stats.sensorRangeMod)
        }
    }

    /** @return 0-1. */
    fun getPercentEffectiveness(fleet: CampaignFleetAPI, objective: SectorEntityToken): Float {
        val fleetCoordinates = fleet.location

        val distance = MathUtils.getDistance(fleetCoordinates, objective.location)
        val minDist = DIST_FOR_MAX_EFFECT
        val adjustedMin = minDist.coerceAtLeast(objective.radius)
        val maxDist = DIST_FOR_MIN_EFFECT
        val adjustedDist = (distance - adjustedMin).coerceAtLeast(0f)
        if (adjustedDist > maxDist) return 0f

        var mult = (1 - (1 / (maxDist / adjustedDist)))
        return mult
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Increases station in-combat sight radius by %s, making it %s.",
            5f,
            Misc.getHighlightColor(),
            "${SIGHT_MULT.trimHangingZero()}x", "near-infinite"
        )
        tooltip.addPara(
            "Provides %s to nearby friendly/trade fleets, with a maximum of %s at %s from the station",
            5f,
            Misc.getHighlightColor(),
            "bonus sensor range", "${MAX_SENSOR_BONUS.toInt()}", "${DIST_FOR_MAX_EFFECT.trimHangingZero()}u"
        )
    }
}