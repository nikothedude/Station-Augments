package niko_SA

import com.fs.starfarer.api.BaseModPlugin
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.Faction
import data.niko_MPC_modPlugin
import data.scripts.campaign.econ.conditions.overgrownNanoforge.industries.overgrownNanoforgeOptionsProvider
import data.utilities.niko_MPC_ids
import data.utilities.niko_MPC_settings
import lunalib.lunaExtensions.getKnownHullmodSpecs
import lunalib.lunaSettings.LunaSettings
import lunalib.lunaSettings.LunaSettingsListener
import niko.MCTE.utils.MCTE_debugUtils
import niko_SA.SA_settings.loadSettings
import niko_SA.augments.core.stationAttachment
import niko_SA.augments.core.stationAugmentStore
import niko_SA.augments.core.stationAugmentStore.getKnownAugments
import niko_SA.augments.threat.SA_threatLootListener
import niko_SA.campaign.*
import niko_SA.codex.CodexData
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

        SA_settings.currentVersion = Global.getSettings().modManager.getModSpec(modId).version
        stationAugmentStore.loadAugmentsFromCSV()
    }

    override fun onGameLoad(newGame: Boolean) {
        super.onGameLoad(newGame)

        SA_settings.MCTE_enabled = Global.getSettings().modManager.isModEnabled("niko_moreCombatTerrainEffects")
        SA_settings.AITweaksEnabled = Global.getSettings().modManager.isModEnabled("aitweaks")
        SA_settings.AOTDVaultsEnabled = Global.getSettings().modManager.isModEnabled("aotd_vok")
        if (SA_settings.AOTDVaultsEnabled) {
            SA_settings.AOTDVaultsVersion = Global.getSettings().modManager.getModSpec("aotd_vok").version
        }
        SA_settings.graphicsLibEnabled = Global.getSettings().modManager.isModEnabled("shaderLib")
        Global.getSector().addTransientListener(SA_stationAugmentDropper())
        Global.getSector().addTransientListener(SA_threatLootListener())
        Global.getSector().listenerManager.addListener(SA_lootListener(), true)
        Global.getSector().listenerManager.addListener(SA_MSLootListener(), true)
        Global.getSector().addTransientListener(SA_augmentMarketAdder())
        Global.getSector().addTransientListener(SA_augmentAutofitter())
        Global.getSector().addTransientListener(SA_augmentSoldListener())
        SA_People.createCharacters()

        val creditsToBuyCore = 2500000f

        Global.getSector().memoryWithoutUpdate["\$SA_DKMACredits"] = creditsToBuyCore
        Global.getSector().memoryWithoutUpdate["\$SA_DKMACreditsDGS"] = Misc.getDGSCredits(creditsToBuyCore)


        CodexData.updateVisibleAugments()
    }

    override fun onAboutToStartGeneratingCodex() {
        super.onAboutToStartGeneratingCodex()
        CodexData.addCodexInfo()
    }

    override fun onAboutToLinkCodexEntries() {
        super.onAboutToLinkCodexEntries()
        CodexData.linkCodexInfo()
    }

    override fun onNewGameAfterEconomyLoad() {
        super.onNewGameAfterEconomyLoad()

        SA_settings.applyPredefinedAugments()
    }

    override fun onNewGame() {
        super.onNewGame()

        SA_specialProcgenHandler.doSpecialProcgen()
    }

    class settingsChangedListener : LunaSettingsListener {
        override fun settingsChanged(modID: String) {
            loadSettings()
        }
    }
}