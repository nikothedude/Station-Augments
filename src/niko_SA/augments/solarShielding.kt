package niko_SA.augments

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_delayedExecution
import niko_SA.SA_fleetUtils.getRepLevelForArrayBonus
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.SA_settings
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent
import org.lazywizard.lazylib.MathUtils

class solarShielding() : stationAttachment(), EveryFrameScript {

    companion object {
        const val CORONA_EFFECT_MULT = 0.2f
        const val ENERGY_DAMAGE_TAKEN_MULT = 0.8f

        const val DIST_FOR_MAX_EFFECT = 400f
        const val DIST_FOR_MIN_EFFECT = 2000f
    }

    val UUID = Misc.genUID()

    override fun getAugmentCost(): Float {
        val superCost = super.getAugmentCost()
        if (SA_settings.MCTE_enabled) {
            return superCost * 1.5f // for obvious reasons
        }
        return superCost
    }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            station.mutableStats.energyDamageTakenMult.modifyMult(id, ENERGY_DAMAGE_TAKEN_MULT)
            station.mutableStats.energyShieldDamageTakenMult.modifyMult(id, ENERGY_DAMAGE_TAKEN_MULT)

            station.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifyMult(id, CORONA_EFFECT_MULT)
        }
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

            fleet.stats.addTemporaryModFlat(0.2f, UUID, "${market!!.name} ${getName()}", 1 - ((1 - CORONA_EFFECT_MULT) * effectLevel), fleet.stats.dynamic.getStat(Stats.CORONA_EFFECT_MULT))
            if (effectLevel >= 1f && !fleet.hasTag(Tags.FLEET_IGNORES_CORONA)) {
                fleet.addTag(Tags.FLEET_IGNORES_CORONA)
                SA_delayedExecution(
                    @JvmSerializableLambda {
                        fleet.removeTag(Tags.FLEET_IGNORES_CORONA)
                    },
                    0.25f,
                    useDays = true,
                    runWhilePaused = false
                ).start()
            }
        }
    }


    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "A few cycles after the advent of solar shielding, sindrian engineers standardized the design into two parts: Orbital, and Spacefaring. " +
            "While significantly harder to star-proof a station (more than they already are), it IS possible, and even offers greater rewards.",
            5f
        )

        val para = tooltip.addPara(
            "Decreases the effect operating in a star's corona has on the station by %s, as well as "  +
            "reducing energy damage taken by %s.",
            5f,
            Misc.getHighlightColor(),
            toPercent(1 - CORONA_EFFECT_MULT), toPercent(1 - ENERGY_DAMAGE_TAKEN_MULT)
        )
        tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
        tooltip.addPara(
            "Fleets operating within %s of the station also receive this bonus, with max effect " +
            "at %s and below.", 0f,
            Misc.getHighlightColor(),
            "${DIST_FOR_MIN_EFFECT.trimHangingZero()}su",
            "${DIST_FOR_MAX_EFFECT.trimHangingZero()}su"
        )
        tooltip.setBulletedListMode(null)

    }

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
}