package niko_SA

import com.fs.starfarer.api.Global
import niko_SA.MarketUtils.addStationAugment
import niko_SA.augments.core.stationAugmentStore.allAugments
import niko_SA.niko_SA_modPlugin.Companion.modId
import org.magiclib.util.MagicSettings
import java.lang.RuntimeException

object SA_settings {

    var MCTE_enabled = false

    fun loadSettings() {

    }

    fun applyPredefinedAugments() {
        //val marketsWithAugments = MagicSettings.getStringMap(modId, "MarketsWithAugments")
        MagicSettings.loadModSettings()
        val settings = MagicSettings.modSettings.getJSONObject(modId)
        val marketsWithAugments = settings.getJSONObject("MarketsWithAugments")

        for (entry in marketsWithAugments.keys()) {
            val marketId = entry.toString()
            val array = marketsWithAugments.getJSONArray(marketId)
            for (i in 0 until array.length()) {
                val augmentId = array.get(i).toString()

                val market = Global.getSector().economy.getMarket(marketId)
                    ?: throw RuntimeException("cannot apply augment id $augmentId to market $marketId - market does not exist!")
                val augment = allAugments[augmentId]?.getInstance?.let { it(market) } ?: throw RuntimeException("augment $augmentId not found in global augment list!")

                market.addStationAugment(augment)
            }
        }
    }
}