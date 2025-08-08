package niko_SA.augments.autofitPlugins

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import niko_SA.SA_mathUtils
import niko_SA.augments.core.stationAugmentSpec

class SpecialModificationsAutofitPlugin() : StationAugmentAutofitPlugin() {

    override fun modifyAutofitWeight(weightToModify: Float, spec: stationAugmentSpec, market: MarketAPI): Float {
        var weight = weightToModify
        if (market.faction.id == Factions.DIKTAT) {
            if (weight == -1f && SA_mathUtils.prob(80)) { // skipped
                return 999f
            }
        }
        return weight
    }
}