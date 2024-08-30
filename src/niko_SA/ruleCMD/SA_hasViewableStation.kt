package niko_SA.ruleCMD

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getStationIndustry

class SA_hasViewableStation: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null) return false

        val market = dialog.interactionTarget.market ?: return false
        return (market.getStationIndustry() != null)
    }
}