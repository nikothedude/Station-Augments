package niko_SA.ruleCMD

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getStationAugments
import niko_SA.MarketUtils.getStationIndustry
import niko_SA.SA_settings

class SA_hasAugmentsToView: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null) return false

        val market = dialog.interactionTarget.market ?: return false
        return (market.getStationAugments().isNotEmpty() || (SA_settings.ALLOW_MODIFY_OF_ALL_STATIONS || market.isPlayerOwned))
    }
}