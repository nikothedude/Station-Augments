package niko_SA.ruleCMD

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.FleetEncounterContext
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl
import com.fs.starfarer.api.impl.campaign.FleetInteractionDialogPluginImpl.FIDConfig
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.FireBest
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_debugUtils
import data.utilities.niko_MPC_dialogUtils
import data.utilities.niko_MPC_fleetUtils.satelliteFleetDespawn
import data.utilities.niko_MPC_satelliteUtils.hasSatellites

class SA_MSCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val command = params[0].getString(memoryMap)

        when (command) {
            "startEncounter" -> {
                val params = FIDConfig()
                params.showFleetAttitude = false
                params.impactsEnemyReputation = false
                params.dismissOnLeave = false
                val plugin = FleetInteractionDialogPluginImpl(params)
                val originalPlugin = dialog.plugin
                val entity = dialog.interactionTarget

                params.delegate = object : FleetInteractionDialogPluginImpl.BaseFIDDelegate() {
                    override fun notifyLeave(dialog: InteractionDialogAPI) {
                        dialog.plugin = originalPlugin
                        dialog.interactionTarget = entity
                        if (plugin.context is FleetEncounterContext) {
                            val context = plugin.context as FleetEncounterContext
                            if (context.didPlayerWinEncounterOutright()) {
                                /*for (handler: niko_MPC_satelliteHandlerCore in )
                                //todo: is the below needed
                                incrementSatelliteGracePeriod(
                                    Global.getSector().playerFleet,
                                    niko_MPC_ids.satellitePlayerVictoryIncrement,
                                    entityFocus
                                ) */
                                FireBest.fire(null, dialog, memoryMap, "SA_moteStationDefeated")
                            } else {
                                dialog.dismiss()
                            }
                        } else {
                            dialog.dismiss()
                        }
                    }
                }

                //dialog.setInteractionTarget(primary)
                val pluginFleet = Misc.getStationFleet(entity.market) ?: return false
                dialog.interactionTarget = pluginFleet
                dialog.plugin = plugin
                plugin.init(dialog)
                //dialog.interactionTarget = entity
            }
            "showFleet" -> {
                val entity = dialog.interactionTarget
                val pluginFleet = Misc.getStationFleet(entity.market) ?: return false
                val playerFleet = Global.getSector().playerFleet
                dialog.visualPanel.showFleetInfo("Your fleet", playerFleet, pluginFleet.name, pluginFleet)
                /*val params = FIDConfig()
                params.justShowFleets = true
                val plugin = FleetInteractionDialogPluginImpl(params)

                //dialog.setInteractionTarget(primary)
                val entity = dialog.interactionTarget
                val pluginFleet = Misc.getStationFleet(entity.market) ?: return false
                dialog.interactionTarget = pluginFleet
                val oldPlugin = dialog.plugin
                dialog.plugin = plugin
                plugin.init(dialog)
                dialog.plugin = oldPlugin
                //dialog.interactionTarget = entity*/
            }
        }

        return false
    }
}