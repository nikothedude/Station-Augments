package niko_SA.augments

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.fleet.RepairTrackerAPI.CREvent
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_fleetUtils.getRepLevelForArrayBonus
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils

class logisticsDrones() : stationAttachment(), EveryFrameScript {
    val affectedMembers = HashMap<FleetMemberAPI, Pair<Float, CREvent>>()

    val UUID = Misc.genUID()

    companion object {
        const val DIST_FOR_MAX_EFFECT = 300f
        const val DIST_FOR_MIN_EFFECT = 700f

        const val CR_BONUS = 15f
        const val ACCESSABILITY_INCREMENT = 0.1f
    }

    override fun applyInCombat(station: ShipAPI) {
        return
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    val interval = IntervalUtil(0.01f, 0.02f) // days

    override fun apply() {
        super.apply()
        market?.primaryEntity?.addScript(this)

        val stationIndustry = getStationIndustry() ?: return
        if (stationIndustry.isFunctional) {
            market?.accessibilityMod?.modifyFlat(id, ACCESSABILITY_INCREMENT, "${stationIndustry.currentName}: ${getName()}")
        }
    }

    override fun unapply() {
        super.unapply()
        market?.primaryEntity?.removeScript(this)

        affectedMembers.toMutableMap().forEach {
            unapplyEffect(it.key)
        }
        affectedMembers.clear()
        market?.accessibilityMod?.unmodify(id)
    }

    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (!interval.intervalElapsed()) return

        val stationEntity = getStationCampaignEntity() ?: return
        val ourFaction = market?.faction ?: return

        for (affectedMember in affectedMembers.toMutableMap()) {
            unapplyEffect(affectedMember.key)
        }
        affectedMembers.clear()

        for (fleet in stationEntity.containingLocation.fleets) {
            if (fleet == getStationFleet()) continue
            val repLevelNeeded = fleet.getRepLevelForArrayBonus()
            if (ourFaction.getRelationshipLevel(fleet.faction) < repLevelNeeded) continue

            var effectLevel = getStationCampaignEntity()?.let { getPercentEffectiveness(fleet, it) } ?: continue

            if (effectLevel <= 0) continue
            fleet.fleetData.membersListCopy.forEach {
                applyEffect(fleet, it, effectLevel)
            }

        }
    }

    private fun applyEffect(fleet: CampaignFleetAPI, member: FleetMemberAPI, mult: Float) {
        val bonus = (CR_BONUS / 100f) * mult
        member.stats.maxCombatReadiness.modifyFlat(UUID, bonus, "${market?.name} ${getName()}")
        //member.repairTracker.cr += bonus

        member.repairTracker.applyCREvent(bonus, UUID, "${market?.name} ${getName()}")
        val event = member.repairTracker.recentEvents.lastOrNull() ?: return
        val pair = Pair(bonus, event)

        affectedMembers[member] = pair
    }

    private fun unapplyEffect(member: FleetMemberAPI) {
        member.stats.maxCombatReadiness.unmodify(UUID)
        val bonus = affectedMembers[member]?.first ?: return

        val event = member.repairTracker.recentEvents.find { it == affectedMembers[member]?.second }
        member.repairTracker.recentEvents -= event

        val oldMaxCr = member.repairTracker.maxCR + bonus
        val toRemove = member.repairTracker.maxCR - oldMaxCr.coerceAtMost(1f)
        member.repairTracker.cr -= -toRemove
        affectedMembers -= member
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
            "Dispatches logistics drones to nearby friendly fleets, which assist in daily routines and crew transfer.",
            5f,
        )
        tooltip.addPara(
            "Increases CR of nearby friendly/trade fleets by %s, with maximum effect at %s.",
            5f,
            Misc.getHighlightColor(),
            "${CR_BONUS.toInt()}%", "${DIST_FOR_MAX_EFFECT.trimHangingZero()}su"
        )

        tooltip.addPara(
            "Additionally, increases market accessibility by %s.",
            5f,
            Misc.getHighlightColor(),
            "${(ACCESSABILITY_INCREMENT * 100f).trimHangingZero()}%"
        )
    }
}