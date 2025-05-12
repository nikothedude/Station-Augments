package niko_SA.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.PlayerMarketTransaction
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import niko_SA.augments.core.stationAugmentStore.getKnownAugments
import niko_SA.augments.core.stationAugmentStore.teachAugment
import niko_SA.specialItems.SA_augmentBlueprintPlugin

class SA_augmentSoldListener: BaseCampaignEventListener(false) {

    override fun reportPlayerMarketTransaction(transaction: PlayerMarketTransaction?) {
        super.reportPlayerMarketTransaction(transaction)
        if (transaction == null) return
        val market = transaction.market ?: return
        val submarket = transaction.submarket ?: return
        var faction: FactionAPI?
        if (submarket.specId == Submarkets.SUBMARKET_BLACK) {
            faction = Global.getSector().getFaction(Factions.PIRATES)
        } else {
            faction = market.faction
        }
        if (faction == null) return

        var didAnything = false
        for (stack in transaction.sold.stacksCopy) {
            if (!stack.isSpecialStack) continue
            if (!stack.specialDataIfSpecial.id.contains("SA_augmentBlueprint")) continue

            val plugin = stack.plugin as? SA_augmentBlueprintPlugin ?: continue
            val augmentSpec = plugin.augment.getSpec()
            if (faction.getKnownAugments().contains(augmentSpec.id)) continue
            faction.teachAugment(augmentSpec.id)
            submarket.cargo.removeItems(CargoAPI.CargoItemType.SPECIAL, stack.data, 1f)
            didAnything = true
        }

        if (didAnything) {
            submarket.cargo.sort()
        }
    }
}