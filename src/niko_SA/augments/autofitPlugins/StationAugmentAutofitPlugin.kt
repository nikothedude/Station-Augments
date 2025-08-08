package niko_SA.augments.autofitPlugins

import com.fs.starfarer.api.campaign.econ.MarketAPI
import niko_SA.augments.core.stationAttachment
import niko_SA.augments.core.stationAugmentSpec

abstract class StationAugmentAutofitPlugin() {
    /** Whatever this returns will be used instead of the pre-existing autofit weight. Returning 0 forbids addition, while returning a huge value nearly guarantees it.*/
    abstract fun modifyAutofitWeight(weightToModify: Float, spec: stationAugmentSpec, market: MarketAPI): Float
}