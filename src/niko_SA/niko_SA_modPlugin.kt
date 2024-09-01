package niko_SA

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeOptionsProvider
import java.lang.RuntimeException

class niko_SA_modPlugin: BaseModPlugin() {

    companion object {
        const val modId = "niko_stationAugments"
    }

    override fun onApplicationLoad() {
        super.onApplicationLoad()

        SA_settings.loadSettings()
        /*val starsectorVers = Global.getSettings().gameVersion
        if (starsectorVers > "0.97a-RC11") {
            throw RuntimeException("CHECK TO SEE IF stationMarketNullPatch IS NECESSARY! https://fractalsoftworks.com/forum/index.php?topic=30567")
        }*/
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        SA_settings.MCTE_enabled = Global.getSettings().modManager.isModEnabled("niko_moreCombatTerrainEffects")
        Global.getSector().listenerManager.addListener(SA_lootListener(), true)

        //Global.getSector().listenerManager.addListener(overgrownNanoforgeOptionsProvider(), true)
    }

    override fun onNewGameAfterEconomyLoad() {
        super.onNewGameAfterEconomyLoad()

        SA_settings.applyPredefinedAugments()
    }
}