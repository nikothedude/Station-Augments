package niko_SA.augments.autofitPlugins

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.LuddicMajority
import com.fs.starfarer.api.impl.campaign.ids.Factions
import niko_SA.augments.core.stationAugmentSpec

class SanctifiedStationAutofitPlugin: StationAugmentAutofitPlugin() {
    override fun modifyAutofitWeight(
        weightToModify: Float,
        spec: stationAugmentSpec,
        market: MarketAPI
    ): Float {
        if (spec.id != "SA_sanctifiedStation") return weightToModify
        if (LuddicMajority.matchesBonusConditions(market)) {
            return 20f  // low chance
        } else if (market.faction.id == Factions.LUDDIC_CHURCH || market.faction.id == Factions.LUDDIC_PATH) {
            return 5f
        }
        return weightToModify
    }
}