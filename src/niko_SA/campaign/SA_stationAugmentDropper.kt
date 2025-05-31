package niko_SA.campaign

import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin
import com.fs.starfarer.api.campaign.SpecialItemData
import niko_SA.MarketUtils.getStationAugments
import niko_SA.SA_fleetUtils.isStationFleet
import niko_SA.scripts.stationMarketTracker
import org.lazywizard.lazylib.MathUtils

class SA_stationAugmentDropper: BaseCampaignEventListener(false) {
    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?) {
        super.reportEncounterLootGenerated(plugin, loot)

        if (plugin == null || loot == null) return

        val battle = plugin.battle
        //val stationFleet = battle.getStationFleet() ?: return
        val stationFleet = battle.snapshotBothSides.firstOrNull { it.isStationFleet() } ?: return
        if (!(stationFleet.isEmpty || stationFleet.isDespawning || stationFleet.isExpired)) return // destroyed, TODO: inaccurate
        val stationMarket = stationMarketTracker.Companion.getInstance().getMarketOfFleet(stationFleet) ?: return
        val augments = stationMarket.getStationAugments()

        for (augment in augments) {
            val dropChance = (augment.getCombatDropChance())
            if ((MathUtils.getRandom().nextFloat() * 100f) >= dropChance) continue

            loot.addSpecial(SpecialItemData("SA_augmentBlueprint", augment.id), 1f)
        }
    }
}