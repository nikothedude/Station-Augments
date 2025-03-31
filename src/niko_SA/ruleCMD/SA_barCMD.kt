package niko_SA.ruleCMD

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.fleet.FleetMemberType
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityEventIntel
import com.fs.starfarer.api.impl.campaign.intel.events.HostileActivityManager
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.submarkets.StoragePlugin
import com.fs.starfarer.api.util.Misc
import lunalib.lunaExtensions.getMarketsCopy
import niko_SA.MarketUtils.addStationAugment
import niko_SA.MarketUtils.getStationIndustry
import niko_SA.MarketUtils.removeStationAugment
import niko_SA.augments.core.stationAugmentStore.getKnownAugments
import niko_SA.augments.core.stationAugmentStore.teachAugment
import niko_SA.augments.moteSink
import kotlin.math.sin

class SA_barCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val market = dialog.interactionTarget?.market ?: return false
        val command = params[0].getString(memoryMap)

        when (command) {
            "canDoDiktatVisit" -> {
                val sink = moteSink.get(market) ?: return false
                if (sink.daysApplied < 15f) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$SA_didDiktatVisitToMote")) return false
                val diktat = Global.getSector().getFaction(Factions.DIKTAT)
                if (diktat.getMarketsCopy().isEmpty()) return false
                if (diktat.isHostileTo(Factions.PLAYER)) return false

                return true
            }
            "soldAugment" -> {
                market.removeStationAugment("SA_moteSinkLow")
                Global.getSector().playerFaction.getKnownAugments() -= "SA_moteSinkLow"

                val sindria = Global.getSector().economy.getMarket("sindria") ?: return false
                Global.getSector().getFaction(Factions.DIKTAT).teachAugment("SA_moteSinkLow")
                sindria.addStationAugment("SA_moteSinkLow")
                sindria.removeStationAugment("SA_highExplosive")
                sindria.getStationIndustry()?.isImproved = true
            }
            "addShips" -> {
                val sindria = Global.getSector().economy.getMarket("sindria") ?: return false
                val storage = sindria.getSubmarket(Submarkets.SUBMARKET_STORAGE)
                (sindria.getSubmarket(Submarkets.SUBMARKET_STORAGE)?.plugin as? StoragePlugin)?.setPlayerPaidToUnlock(true)

                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "executor_Hull", null)
                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "executor_Hull", null)
                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "executor_Hull", null)

                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "eagle_LG_Hull", null)
                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "eagle_LG_Hull", null)
                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "eagle_LG_Hull", null)
                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "eagle_LG_Hull", null)
                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "falcon_LG_Hull", null)

                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "hammerhead_LG_Hull", null)
                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "hammerhead_LG_Hull", null)
                storage.cargo.addMothballedShip(FleetMemberType.SHIP, "sunder_LG_Hull", null)

                storage.cargo.initMothballedShips(Factions.PLAYER)
            }
        }

        return false
    }
}