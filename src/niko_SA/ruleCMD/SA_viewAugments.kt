package niko_SA.ruleCMD

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getStationIndustry
import niko_SA.augments.core.AugmentMenuDialogueDelegate

class SA_viewAugments: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null) return false

        val market = dialog.interactionTarget.market ?: return false
        val stationIndustry = market.getStationIndustry() ?: return false
        val delegate = AugmentMenuDialogueDelegate(stationIndustry)
        // 2 main modes: see existing hullmods, and modify hullmods
        dialog.showCustomDialog(AugmentMenuDialogueDelegate.WIDTH, AugmentMenuDialogueDelegate.HEIGHT, delegate)

        return true
    }
}