package niko_SA.augments.core

import com.fs.starfarer.api.campaign.econ.MarketAPI

/** A store for constant data, and a instantiation method of the station augment. */
class stationAugmentData(
    val getInstance: (market: MarketAPI?) -> stationAttachment,
    val knownToAllFactionsByDefault: Boolean = false,
    /** dropgroupid -> weight. Used for determining which augment BP to drop. */
    val dropGroupWeights: MutableMap<String, Float>
) {
}