package niko_SA.augments.threat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.EngagementResultForFleetAPI
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.listeners.ShowLootListener
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.impl.campaign.ids.Tags
import niko_SA.MarketUtils.getStationAugments
import niko_SA.SA_fleetUtils.isStationFleet
import niko_SA.SA_ids
import niko_SA.SA_mathUtils
import niko_SA.scripts.stationMarketTracker
import org.lazywizard.lazylib.MathUtils

class SA_threatLootListener: BaseCampaignEventListener(false) {

    override fun reportEncounterLootGenerated(plugin: FleetEncounterContextPlugin?, loot: CargoAPI?) {
        super.reportEncounterLootGenerated(plugin, loot)

        if (plugin == null || loot == null) return

        val battle = plugin.battle
        val playerFleet = Global.getSector().playerFleet
        //val stationFleet = battle.getStationFleet() ?: return
        if (plugin.loser == playerFleet) return
        for (wpn in loot.weapons) {
            if (wpn.item == "kinetic_fragments" && SA_mathUtils.prob(40)) {
                loot.addSpecial(
                    SpecialItemData("SA_augmentBlueprint", "SA_kineticFragments"),
                    1f
                )
                continue
            }
            if (wpn.item == "swarm_launcher" && SA_mathUtils.prob(50)) {
                loot.addSpecial(
                    SpecialItemData("SA_augmentBlueprint", "SA_attackSwarms"),
                    1f
                )
                continue
            }
            if (wpn.item == "seeker_fragment" && SA_mathUtils.prob(40)) {
                loot.addSpecial(
                    SpecialItemData("SA_augmentBlueprint", "SA_seekerFragments"),
                    1f
                )
                continue
            }
            if (wpn.item == "devouring_swarm" && SA_mathUtils.prob(40)) {
                loot.addSpecial(
                    SpecialItemData("SA_augmentBlueprint", "SA_defabSwarms"),
                    1f
                )
                continue
            }
        }
        for (hmod in loot.stacksCopy.filter { it.isSpecialStack && it.specialDataIfSpecial.id == "modspec" }) {
            val data = hmod.specialDataIfSpecial
            if (data.data == HullMods.FRAGMENT_SWARM && SA_mathUtils.prob(70)) {
                loot.addSpecial(
                    SpecialItemData("SA_augmentBlueprint", "SA_fragmentSwarm"),
                    1f
                )
                continue
            }
            if (data.data == HullMods.SECONDARY_FABRICATOR) {
                if (SA_mathUtils.prob(70)) {
                    loot.addSpecial(
                        SpecialItemData("SA_augmentBlueprint", "SA_secondaryFabricator"),
                        1f
                    )
                }
                if (SA_mathUtils.prob(50)) {
                    loot.addSpecial(
                        SpecialItemData("SA_augmentBlueprint", "SA_constructionSwarms"),
                        1f
                    )
                }
                continue
            }
            if (data.data == HullMods.FRAGMENT_COORDINATOR && SA_mathUtils.prob(70)) {
                loot.addSpecial(
                    SpecialItemData("SA_augmentBlueprint", "SA_fragmentCoordinator"),
                    1f
                )
                continue
            }
        }
    }
}