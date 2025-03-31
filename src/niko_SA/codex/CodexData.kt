package niko_SA.codex

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModSpecAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.codex.*
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin.ListMode
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import niko.MCTE.codex.CodexData.TERRAIN
import niko_SA.SA_ids
import niko_SA.augments.core.stationAttachment
import niko_SA.augments.core.stationAugmentStore
import niko_SA.niko_SA_modPlugin
import kotlin.math.max

object CodexData {
    const val STATION_AUGMENTS_CAT = "SA_stationAugments"
    lateinit var AUGMENT_CAT: CodexEntryV2

    fun addCodexInfo() {
        val augmentCat = object : CodexEntryV2(STATION_AUGMENTS_CAT, "Station Augments", CodexDataV2.getIcon(STATION_AUGMENTS_CAT)) {
            override fun hasTagDisplay(): Boolean {
                return false //TODO - we SHOULD have tags
            }

            override fun getSourceMod(): ModSpecAPI? {
                return Global.getSettings().modManager.getModSpec(niko_SA_modPlugin.modId)
            }
        }
        CodexDataV2.ROOT.addChild(augmentCat)
        AUGMENT_CAT = augmentCat

        for (augmentSpec in stationAugmentStore.allAugments.values) {
            if (augmentSpec.codexTags.contains(Tags.HIDE_IN_CODEX)) continue
            val augmentInstance = augmentSpec.getNewPluginInstance(null)
            val entry = object : CodexEntryV2("${augmentSpec.id}_augCodEntry", augmentInstance.getName(), augmentInstance.getImageName(), augmentInstance) {

                override fun getTags(): MutableSet<String> {
                    return (param as stationAttachment).getSpec().codexTags
                }

                override fun isUnlockedIfRequiresUnlock(): Boolean {
                    return getSeenAugments().contains((param as stationAttachment).id)
                }

                override fun getUnlockRelatedTags(): MutableSet<String> {
                    return (param as stationAttachment).getSpec().codexTags
                }

                override fun createTitleForList(info: TooltipMakerAPI, width: Float, mode: ListMode?) {
                    val ourAugment = param as stationAttachment
                    info.addPara(ourAugment.getName(), Misc.getBasePlayerColor(), 0f)
                    if (mode == ListMode.RELATED_ENTRIES) {
                        info.addPara("Station Augment", Misc.getGrayColor(), 0f)
                    }
                }

                override fun hasCustomDetailPanel(): Boolean = true

                override fun createCustomDetail(
                    panel: CustomPanelAPI?,
                    relatedEntries: UIPanelAPI?,
                    codex: CodexDialogAPI?
                ) {
                    if (panel == null) return

                    val opad = 10f
                    val width = panel.position.width
                    val horzBoxPad = 30f
                    // the right width for a tooltip wrapped in a box to fit next to relatedEntries
                    val tw = width - 290f - opad - horzBoxPad + 10f
                    val ourAugment = param as stationAttachment

                    val text = panel.createUIElement(tw, 0f, false)
                    text.setParaSmallInsignia()
                    ourAugment.getBasicDescription(text, false)
                    panel.updateUIElementSizeAndMakeItProcessInput(text)
                    val box = panel.wrapTooltipWithBox(text)
                    panel.addComponent(box).inTL(0f, 0f)
                    if (relatedEntries != null) {
                        panel.addComponent(relatedEntries).inTR(0f, 0f)
                    }

                    var height = box.position.height
                    if (relatedEntries != null) {
                        height = max(height.toDouble(), relatedEntries.position.height.toDouble()).toFloat()
                    }
                }

                override fun getSourceMod(): ModSpecAPI? {
                    return Global.getSettings().modManager.getModSpec(niko_SA_modPlugin.modId)
                }
            }
            augmentCat.addChild(entry)
        }
    }

    private fun getSeenAugments(): MutableSet<String> {
        if (Global.getSector().memoryWithoutUpdate[SA_ids.CODEX_KNOWN_AUGMENTS] !is MutableSet<*>) {
            Global.getSector().memoryWithoutUpdate[SA_ids.CODEX_KNOWN_AUGMENTS] = HashSet<String>()
        }
        return Global.getSector().memoryWithoutUpdate[SA_ids.CODEX_KNOWN_AUGMENTS] as MutableSet<String>
    }

    fun linkCodexInfo() {

    }

    fun updateVisibleAugments() {
        for (entry in AUGMENT_CAT.children) {
            if (!entry.isVisible || entry.isLocked) continue
            val augment = (entry.param as? stationAttachment) ?: continue
            val spec = augment.getSpec()

            if (!getSeenAugments().contains(spec.id)) {
                getSeenAugments() += spec.id
            }
        }
    }

    @JvmStatic
    fun unlockAugment(id: String) {
        if (getSeenAugments().contains(id)) return
        getSeenAugments() += id
        CodexIntelAdder.get().addEntry(getAugmentEntryId(id))
    }

    fun getAugmentEntryId(base: String): String = "${base}_augCodEntry"
}