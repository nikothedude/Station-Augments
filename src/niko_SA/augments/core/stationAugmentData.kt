package niko_SA.augments.core

import com.fs.starfarer.api.campaign.econ.MarketAPI

/** A store for constant data, and a instantiation method of the station augment. */
class stationAugmentData(
    val getInstance: (market: MarketAPI?) -> stationAttachment,
    /** If SA_ids.ALL_FACTIONS is used, is considered to be known by all factions.
     *  Note - this is only by default, and is currently only used for determining who will sell the augments on their markets. */
    val factionsThatKnowByDefault: MutableSet<String> = HashSet(),
    /** dropgroupid -> weight. Used for determining which augment BP to drop. */
    val dropGroupWeights: MutableMap<String, Float>,
    /** Weight for this augment to be sold at a market, per roll. If 0, will never be sold. */
    val sellWeight: Float = 1f
) {
}