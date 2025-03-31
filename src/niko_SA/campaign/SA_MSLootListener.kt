package niko_SA.campaign

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SpecialItemData
import com.fs.starfarer.api.campaign.SpecialItemPlugin
import com.fs.starfarer.api.campaign.listeners.ShowLootListener
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_ids

class SA_MSLootListener: ShowLootListener {
    override fun reportAboutToShowLootToPlayer(loot: CargoAPI?, dialog: InteractionDialogAPI?) {
        if (loot == null || dialog == null) return
        if (dialog.interactionTarget == null) return

        if (!dialog.interactionTarget.memoryWithoutUpdate.getBoolean("\$SA_moteStation")) return

        loot.addSpecial(SpecialItemData("SA_augmentBlueprint", "SA_moteSinkLow"), 1f)
    }
}