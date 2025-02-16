package niko_SA.ruleCMD

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import niko_SA.campaign.SA_TTBlackSiteBreadcrumbIntel

class SA_breadcrumbCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val market = dialog.interactionTarget?.market ?: return false
        val command = params[0].getString(memoryMap)

        when (command) {
            "canInit" -> {
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$SA_didMSBreadcrumb")) return false
                if (Global.getSector().memoryWithoutUpdate.getBoolean("\$SA_exploredMSOne")) return false
                if (Global.getSector().memoryWithoutUpdate["\$MPC_SATTBlackSiteTwo"] == null) return false
                if (Global.getSector().playerPerson.stats.level < 10) return false
                if (market.factionId != Factions.INDEPENDENT && market.factionId != Factions.TRITACHYON) return false

                return true
            }
            "startBreadcrumb" -> {
                val system = Global.getSector().memoryWithoutUpdate["\$MPC_SATTBlackSiteTwo"] as? StarSystemAPI ?: return false
                val intel = SA_TTBlackSiteBreadcrumbIntel(system.hyperspaceAnchor)
                Global.getSector().intelManager.addIntel(intel, false, dialog.textPanel)
            }
        }

        return false
    }
}