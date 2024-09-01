package niko_SA

import com.fs.starfarer.api.campaign.CargoAPI
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.listeners.ShowLootListener
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.procgen.SalvageEntityGenDataSpec.DropData
import com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageEntityGeneratorOld
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.SalvageEntity
import com.fs.starfarer.api.util.Misc

// taken from bionic alteration
class SA_lootListener: ShowLootListener {
    override fun reportAboutToShowLootToPlayer(loot: CargoAPI?, dialog: InteractionDialogAPI?) {
        if (loot == null || dialog == null) return
        if (dialog.interactionTarget == null) return

        val dropData = getDropDataFromEntity(dialog.interactionTarget)

        val memory = dialog.interactionTarget.memoryWithoutUpdate
        val randomSeed = memory.getLong(MemFlags.SALVAGE_SEED)
        val random = Misc.getRandom(randomSeed, 100)

        val dropValue: List<DropData> = generateDropValueList(dropData)
        val dropRandom: List<DropData> = generateDropRandomList(dropData)

        val salvage = SalvageEntity.generateSalvage(
            random,
            1f, 1f, 1f, 1f, dropValue, dropRandom
        )
        loot.addAll(salvage)
    }

    /**
     * To create drop value for the custom drop group
     * @param dropData
     * @return
     */
    private fun generateDropValueList(dropData: List<DropData>): List<DropData> {
        val dropValueList: MutableList<DropData> = java.util.ArrayList()
        for (d in dropData) {
            if (d.group == null) continue
            if (d.value == -1) continue
            var value = -1f
            if (d.group.contains("rare_tech")) {
                value = (d.value * 0.2f)
            }
            /*if (d.group.contains("goods")) {
                value = (d.value * 0.05f).toInt()
            }
            if (d.group.contains("supply")) {
                value = (d.value * 0.08f).toInt()
            }
            if (d.group.contains("machinery")) {
                value = (d.value * 0.14f).toInt()
            }
            if (d.group.contains("freighter_cargo")) {
                value = (d.value * 0.1f).toInt()
            }*/
            if (d.group.contains("blueprints")) {
                value = (d.value * 0.2f)
            }
            if (d.group.contains("hullmod")) {
                value = (d.value * 0.04f)
            }
            if (value != -1f) {
                val dropOne = DropData()
                dropOne.group = "SA_augmentNormal"
                dropOne.valueMult = d.valueMult
                dropOne.value = value.toInt()
                dropValueList.add(dropOne)

                val dropTwo = DropData()
                dropTwo.group = "SA_augmentRare"
                dropTwo.valueMult = d.valueMult
                dropTwo.value = value.toInt()
                dropValueList.add(dropTwo)
            }
        }
        return dropValueList
    }

    /**
     * To create drop random for the custom drop group: ba_bionic_civil, ba_bionic_military...
     * @param dropData
     * @return
     */
    private fun generateDropRandomList(dropData: List<DropData>): List<DropData> {
        val dropRandomList: MutableList<DropData> = java.util.ArrayList()
        for (d in dropData) {
            if (d.group == null) continue
            if (d.chances == -1) continue
            var chances = -1f
            if (d.group.contains("rare_tech")) {
                chances = (d.chances * 1f)
            }
            /*if (d.group.contains("goods")) {
                chances = (d.chances * 0.2f).toInt()
            }
            if (d.group.contains("supply")) {
                chances = (d.chances * 0.6f).toInt()
            }
            if (d.group.contains("machinery")) {
                chances = (d.chances * 0.8f).toInt()
            }
            if (d.group.contains("freighter_cargo")) {
                chances = (d.chances * 0.4f).toInt()
            }*/
            if (d.group.contains("blueprints")) {
                chances = (d.chances * 1f)
            }
            if (d.group.contains("hullmod")) {
                chances = (d.chances * 0.12f)
            }
            if (chances != -1f) {

                val dropOne = DropData()
                dropOne.group = "SA_augmentNormal"
                dropOne.maxChances = d.maxChances
                dropOne.chances = chances.toInt()
                dropRandomList.add(dropOne)

                val dropTwo = DropData()
                dropTwo.group = "SA_augmentRare"
                dropTwo.maxChances = d.maxChances
                dropTwo.chances = chances.toInt()
                dropRandomList.add(dropTwo)
            }
        }
        return dropRandomList
    }

    fun getDropDataFromEntity(entity: SectorEntityToken): List<DropData> {
        val dropData: MutableList<DropData> = ArrayList()

        //first get drops assigned directly to entity
        if (entity.dropRandom != null) {
            dropData.addAll(entity.dropRandom)
        }
        if (entity.dropValue != null) {
            dropData.addAll(entity.dropValue)
        }

        //then try to get spec from entity and the spec's drops
        var specId = entity.customEntityType
        if (specId == null || entity.memoryWithoutUpdate.contains(MemFlags.SALVAGE_SPEC_ID_OVERRIDE)) {
            specId = entity.memoryWithoutUpdate.getString(MemFlags.SALVAGE_SPEC_ID_OVERRIDE)
        }
        if (specId != null
            && SalvageEntityGeneratorOld.hasSalvageSpec(specId)
        ) {
            val spec = SalvageEntityGeneratorOld.getSalvageSpec(specId)

            //get drop randoms from that spec
            if (spec != null && spec.dropRandom != null) {
                dropData.addAll(spec.dropRandom)
            }
            if (spec != null && spec.dropValue != null) {
                dropData.addAll(spec.dropValue)
            }
        }
        return dropData
    }
}