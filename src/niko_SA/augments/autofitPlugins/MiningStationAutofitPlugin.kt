package niko_SA.augments.autofitPlugins

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAugmentSpec
import niko_SA.augments.miningStation
import kotlin.math.min

class MiningStationAutofitPlugin: StationAugmentAutofitPlugin() {

    companion object {
        const val OUTPUT_PER_WEIGHT = 3f
    }

    override fun modifyAutofitWeight(
        weightToModify: Float,
        spec: stationAugmentSpec,
        market: MarketAPI
    ): Float {
        var weight = weightToModify
        if (weight == -1f) { // skipped
            if ((market.faction.id == Factions.PLAYER || market.isPlayerOwned) && market.industries.any { it.spec.hasTag(Industries.MINING) }) {
                return weight // lets just assume theyre already mining the asteroids
            }
            val ind = Misc.getStationIndustry(market) as? OrbitalStation ?: return weight
            val fleet = ind.stationFleet ?: return weight
            val entity = ind.stationEntity ?: return weight

            val output = miningStation.getOutput(entity, ind, market, fleet)

            val cumulativeOutput = output.values.sum()
            weight = cumulativeOutput * OUTPUT_PER_WEIGHT
            val strength = miningStation.getMiningStrength(ind, market, fleet)
            if (strength >= miningStation.MINING_STRENGTH_TO_SUPPRESS_METEORS && market.hasCondition(Conditions.METEOR_IMPACTS)) {
                weight += 30f
            }
        }
        return weight
    }
}