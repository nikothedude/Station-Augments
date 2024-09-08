package niko_SA.augments

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.fleet.FleetMemberAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_fleetUtils.getRepLevelForArrayBonus
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils

class navRelay(market: MarketAPI?, id: String): stationAttachment(market, id), EveryFrameScript {

    companion object {
        const val NAV_BONUS = 20f

        const val BURN_BONUS = 1f
        const val MAX_DIST_FROM_STATION_FOR_BONUS = 6000f
    }

    override val augmentCost: Float = 9f
    override val name: String = "Nav Relay"
    override val spriteId: String = "graphics/hullmods/nav_relay.png"
    val UUID = Misc.genUID()

    override fun applyInCombat(station: ShipAPI) {
        station.mutableStats.dynamic.getMod(Stats.COORDINATED_MANEUVERS_FLAT).modifyFlat(id, NAV_BONUS)
    }

    override fun apply() {
        super.apply()
        market?.primaryEntity?.addScript(this)
    }

    override fun unapply() {
        super.unapply()
        market?.primaryEntity?.removeScript(this)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "Increases in-combat nav rating by %s.",
            5f,
            Misc.getHighlightColor(),
            "${NAV_BONUS.toInt()}%"
        )

        tooltip.addPara(
            "Increases max burn of friendly/trade fleets within %s of the station by %s.",
            5f,
            Misc.getHighlightColor(),
            "${MAX_DIST_FROM_STATION_FOR_BONUS.toInt()}su", "${BURN_BONUS.toInt()}"
        )
    }

    override fun isDone(): Boolean = false
    override fun runWhilePaused(): Boolean = false

    val interval = IntervalUtil(0.05f, 0.08f) // days
    override fun advance(amount: Float) {
        val days = Misc.getDays(amount)
        interval.advance(days)
        if (!interval.intervalElapsed()) return

        val stationEntity = getStationCampaignEntity() ?: return
        val ourFaction = market?.faction ?: return
        for (fleet in stationEntity.containingLocation.fleets) {
            val dist = MathUtils.getDistance(fleet, stationEntity)
            if (dist > MAX_DIST_FROM_STATION_FOR_BONUS) continue

            val repLevelNeeded = fleet.getRepLevelForArrayBonus()
            if (ourFaction.getRelationshipLevel(fleet.faction) < repLevelNeeded) continue

            fleet.stats.addTemporaryModFlat(0.2f, UUID, "${market.name} $name",BURN_BONUS, fleet.stats.fleetwideMaxBurnMod)
        }
    }
}