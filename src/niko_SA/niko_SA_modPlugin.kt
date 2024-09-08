package niko_SA

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import data.niko_MPC_modPlugin
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeOptionsProvider
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_settings
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.utils.MCTE_debugUtils
import niko_SA.SA_settings.loadSettings
import org.apache.log4j.Level
import org.lazywizard.lazylib.MathUtils
import java.lang.RuntimeException

class niko_SA_modPlugin: BaseModPlugin() {

    companion object {
        const val modId = "niko_stationAugments"
    }

    override fun onApplicationLoad() {
        super.onApplicationLoad()

        loadSettings()
        LunaSettings.addSettingsListener(settingsChangedListener())
        /*val starsectorVers = Global.getSettings().gameVersion
        if (starsectorVers > "0.97a-RC11") {
            throw RuntimeException("CHECK TO SEE IF stationMarketNullPatch IS NECESSARY! https://fractalsoftworks.com/forum/index.php?topic=30567")
        }*/
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        SA_settings.MCTE_enabled = Global.getSettings().modManager.isModEnabled("niko_moreCombatTerrainEffects")
        Global.getSector().addTransientListener(SA_stationAugmentDropper())
        Global.getSector().listenerManager.addListener(SA_lootListener(), true)

        /*if (Global.getSector().memoryWithoutUpdate[SA_ids.SA_nextAugmentBlueprintSeedMemId] == null) {
            Global.getSector().memoryWithoutUpdate[SA_ids.SA_nextAugmentBlueprintSeedMemId] = MathUtils.getRandom().nextLong()
        }*/

        //Global.getSector().listenerManager.addListener(overgrownNanoforgeOptionsProvider(), true)
    }

    override fun onNewGameAfterEconomyLoad() {
        super.onNewGameAfterEconomyLoad()

        SA_settings.applyPredefinedAugments()
    }

    class settingsChangedListener : LunaSettingsListener {
        override fun settingsChanged(modID: String) {
            loadSettings()
        }
    }
}