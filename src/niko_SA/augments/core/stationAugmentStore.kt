package niko_SA.augments.core

import com.fs.starfarer.api.campaign.econ.MarketAPI
import niko_SA.augments.axialOverclocking

object stationAugmentStore {
    /** The global store of all augments in the game. Make sure to modify this if adding a new augment.
     * If youre looking to add an augment as a third-party mod author, you can modify this on application load. */
    val allAugments = HashMap<String, stationAugmentData>()

    init {
        allAugments["SA_axialOverclocking"] = stationAugmentData { market: MarketAPI -> axialOverclocking(market, "SA_axialOverclocking") }
    }
}