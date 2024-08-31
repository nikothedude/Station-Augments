package niko_SA.augments.core

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getRemainingAugmentBudget

/** A store for constant data, and a instantiation method of the station augment. */
class stationAugmentData(
    val getInstance: (market: MarketAPI) -> stationAttachment,
    val knownToAllFactionsByDefault: Boolean = false
) {
}