package niko_SA.augments.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModSpecAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.loading.WithSourceMod
import niko_SA.augments.autofitPlugins.StationAugmentAutofitPlugin
import niko_SA.campaign.SA_augmentAutofitter
import java.awt.Color

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
    var requiredItemId: String?,
    var autofitPlugin: StationAugmentAutofitPlugin?,
    var nameColor: Color,
    val modId: String,
    val reqModIds: MutableSet<String>
): WithSourceMod {

    fun getNewPluginInstance(market: MarketAPI?): stationAttachment {
        val new = Global.getSettings().scriptClassLoader.loadClass(pluginPath).newInstance() as stationAttachment
        new.market = market
        new.id = id
        new.init()
        return new
    }

    override fun getSourceMod(): ModSpecAPI? {
        return Global.getSettings().modManager.getModSpec(modId)
    }
}