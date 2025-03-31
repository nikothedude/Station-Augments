package niko_SA.augments.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI

/** A store for constant data, and a instantiation method of the station augment. */
class stationAugmentSpec(
    var id: String,
    var name: String,
    val knowledgeTags: MutableSet<String>,
    val usageTags: MutableSet<String>,
    val codexTags: MutableSet<String>,
    val manufacturer: String,
    /** Used for instantiation. Will crash if this is wrong. */
    var pluginPath: String,
    /** Higher = more chance to randomly drop from loot. */
    var dropWeight: Float,
    var dropCombatWeight: Float,
    var sellWeight: Float,
    var spritePath: String,
    val apCost: Float,
) {
    fun getNewPluginInstance(market: MarketAPI?): stationAttachment {
        val new = Global.getSettings().scriptClassLoader.loadClass(pluginPath).newInstance() as stationAttachment
        new.market = market
        new.id = id
        new.init()
        return new
    }
}