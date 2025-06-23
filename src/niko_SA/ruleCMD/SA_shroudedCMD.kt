package niko_SA.ruleCMD

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.TextPanelAPI
import com.fs.starfarer.api.campaign.impl.items.ShroudedHullmodItemPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin
import com.fs.starfarer.api.impl.campaign.rulecmd.missions.BarCMD
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getStationAugments
import niko_SA.SA_delayedExecution
import niko_SA.augments.core.stationAugmentStore.allAugments
import niko_SA.augments.core.stationAugmentStore.teachAugment

class SA_shroudedCMD: BaseCommandPlugin() {
    override fun execute(
        ruleId: String?,
        dialog: InteractionDialogAPI?,
        params: MutableList<Misc.Token>?,
        memoryMap: MutableMap<String, MemoryAPI>?
    ): Boolean {
        if (dialog == null || params == null) return false

        val market = dialog.interactionTarget?.market
        val command = params[0].getString(memoryMap)

        when (command) {
            "hasShroudedAugment" -> {
                if (market == null) return false
                for (augment in market.getStationAugments()) {
                    val spec = augment.getSpec()
                    if (spec.knowledgeTags.contains(Tags.SHROUDED)) return true
                }
            }
            "stopBarAmbience" -> {
                BarCMD.getAmbiencePlayer()?.stop()
            }
            "unlockAugment" -> {
                val modId = params[1].getString(memoryMap)
                SA_delayedExecution(
                    @JvmSerializableLambda {
                        val augmentSpec = allAugments[modId]
                        if (augmentSpec != null) {
                            //Global.getSoundPlayer().playUISound("ui_acquired_hullmod", 1f, 1f)
                            val text: TextPanelAPI? = dialog.textPanel
                            text?.setFontSmallInsignia()
                            val str = augmentSpec.name
                            text?.addParagraph("Acquired station augment: $str", Misc.getPositiveHighlightColor())
                            text?.highlightInLastPara(Misc.getHighlightColor(), str)
                            text?.setFontInsignia()
                        }
                    },
                    0f,
                    true,
                    useDays = false
                ).start()

                Global.getSector().playerFaction.teachAugment(modId)
            }
        }

        return false
    }
}