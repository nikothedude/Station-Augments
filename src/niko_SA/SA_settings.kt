package niko_SA

import com.fs.starfarer.api.Global
import lunalib.lunaSettings.LunaSettings
import niko_SA.MarketUtils.addStationAugment
import niko_SA.augments.core.stationAugmentStore.allAugments
import niko_SA.niko_SA_modPlugin.Companion.modId
import org.magiclib.util.MagicSettings
import java.lang.RuntimeException

object SA_settings {

    var IS_APPLYING_PREDEFINED_AUGMENTS = false

    @JvmStatic
    var isWindows = System.getProperty("os.name").contains("Windows")

    @JvmStatic
    lateinit var currentVersion: String

    @JvmStatic
    var SOTF_enabled = false
    @JvmStatic
    var ASAS_enabled = false
    @JvmStatic
    var MCTE_enabled = false
    @JvmStatic
    var AITweaksEnabled = false
    @JvmStatic
    var AOTDVaultsEnabled = false
    @JvmStatic
    var AOTDVaultsVersion = ""

    @JvmStatic
    var graphicsLibEnabled = false

    @JvmStatic
    var AUTOFIT_ENABLED = true

    @JvmStatic
    var ALLOW_MODIFY_OF_ALL_STATIONS = false
    @JvmStatic
    var BASE_STATION_AUGMENT_BUDGET = 20f
    @JvmStatic
    var ALLOW_FP_RATIO_VIEWING = false

    fun loadSettings() {
        ALLOW_MODIFY_OF_ALL_STATIONS = LunaSettings.getBoolean(modId, "SA_allowAlwaysModifyAugments")!!
        BASE_STATION_AUGMENT_BUDGET = LunaSettings.getFloat(modId, "SA_baseStationAugmentBudget")!!
        AUTOFIT_ENABLED = LunaSettings.getBoolean(modId, "SA_autofitEnabled")!!
        ALLOW_FP_RATIO_VIEWING = LunaSettings.getBoolean(modId, "SA_allowFPRatioViewing")!!
    }

    fun applyPredefinedAugments() {
        IS_APPLYING_PREDEFINED_AUGMENTS = true
        //val marketsWithAugments = MagicSettings.getStringMap(modId, "MarketsWithAugments")
        MagicSettings.loadModSettings()
        val settings = MagicSettings.modSettings.getJSONObject(modId)
        val marketsWithAugments = settings.getJSONObject("MarketsWithAugments")

        for (entry in marketsWithAugments.keys()) {
            val marketId = entry.toString()
            val market = Global.getSector().economy.getMarket(marketId)
            if (market == null) {
                SA_debugUtils.log.error("Did not find market of id $marketId, aborting augment addition")
                break
            }
            val array = marketsWithAugments.getJSONArray(marketId)
            for (i in 0 until array.length()) {
                val augmentId = array.get(i).toString()

                val augment = allAugments[augmentId]?.getNewPluginInstance(market)
                if (augment == null) {
                    SA_debugUtils.log.error("Invalid augment id: $augmentId!")
                    continue
                }

                augment.considerReqItem = false
                market.addStationAugment(augment)
                augment.considerReqItem = true
            }
        }
        IS_APPLYING_PREDEFINED_AUGMENTS = false
    }
}