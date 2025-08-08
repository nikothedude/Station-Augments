package niko_SA.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCampaignEventListener
import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.CargoAPI.CargoItemType
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.SubmarketPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Submarkets
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin
import com.fs.starfarer.api.util.WeightedRandomPicker
import niko_SA.augments.core.stationAugmentStore.allAugments
import niko_SA.augments.core.stationAugmentStore.getKnownAugments

/** Adds augments to submarkets for purchase. */
class SA_augmentMarketAdder: BaseCampaignEventListener(false) {

    companion object {
        const val TIMES_TO_PICK_PER_ROLL = 3f
    }

    override fun reportPlayerOpenedMarketAndCargoUpdated(market: MarketAPI?) {
        super.reportPlayerOpenedMarketAndCargoUpdated(market)

        if (market == null) return

        for (submarket in market.submarketsCopy) {
            if (submarket.specId == Submarkets.SUBMARKET_STORAGE) continue
            if (submarket.plugin !is BaseSubmarketPlugin) continue
            if ((submarket.plugin as BaseSubmarketPlugin).sinceSWUpdate > 0.001f) continue
            // the below is necessary since other mods, namely indevo, plug into this and set sinceSWUpdate to 0.001f
            if (market.memoryWithoutUpdate.getBoolean("\$SA_doNotUpdateAugments_${submarket.specId}")) continue

            addAugments(submarket, market)
        }
    }

    private fun addAugments(submarket: SubmarketAPI, market: MarketAPI) {
        val cargo = submarket.cargo
        for (stack in cargo.stacksCopy) {
            if (stack.isSpecialStack && stack.specialDataIfSpecial.id.contains("SA_augmentBlueprint")) {
                cargo.removeStack(stack)
            }
        }
        val faction = market.faction

        val knownAugments = faction.getKnownAugments().toMutableSet()
        if (submarket.specId == Submarkets.SUBMARKET_BLACK) {
            knownAugments += Global.getSector().getFaction(Factions.PIRATES).getKnownAugments()
        }
        val picker = WeightedRandomPicker<String>()
        var totalWeight = 0f
        for (entry in knownAugments) {
            val data = allAugments[entry] ?: continue
            if (data.sellWeight <= 0f) continue
            picker.add(entry, data.sellWeight)
            totalWeight += data.sellWeight
        }
        picker.add("nothing", totalWeight * 4f)

        var picksLeft = TIMES_TO_PICK_PER_ROLL + market.size
        if (submarket.specId == "exerelin_prismMarket") {
            picksLeft *= 4f
        }
        while (picksLeft-- > 0f) {
            val picked = picker.pick()
            if (picked == "nothing") continue

            cargo.addSpecial(SpecialItemData("SA_augmentBlueprint", picked), 1f)
        }
        (submarket.plugin as? BaseSubmarketPlugin)?.sinceSWUpdate = 0.001f
        market.memoryWithoutUpdate.set("\$SA_doNotUpdateAugments_${submarket.specId}", true, 0f)
    }
}