package niko_SA.codex

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.ModSpecAPI
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.codex.*
import com.fs.starfarer.api.impl.codex.CodexEntryPlugin.ListMode
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.ui.UIPanelAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_ids
import niko_SA.augments.core.stationAttachment
import niko_SA.augments.core.stationAugmentStore
import niko_SA.augments.core.stationAugmentStore.getKnownAugments
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
            val entry = object : CodexEntryV2(getAugmentEntryId(augmentInstance.id), augmentInstance.getName(), augmentInstance.getImageName(), augmentInstance) {

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
                    ourAugment.getBasicDescription(text, false, panel)
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


                    val textTwo = panel.createUIElement(tw, 0f, false)
                    textTwo.addSpacer(5f)

                    val sector = Global.getSector()
                    if (sector != null) {
                        var addedOneAlready = false
                        var beginningOfRow: UIPanelAPI? = null
                        for (faction in Global.getSector().allFactions.filter { it.isShowInIntelTab }) {
                            if (augmentInstance.id !in faction.getKnownAugments()) continue

                            val imageTooltip = textTwo.beginImageWithText(
                                faction.crest,
                                25f,
                                150f,
                                true
                            )
                            val fixedName = faction.displayName.replaceFirstChar { it.uppercaseChar() }
                            val imageLabel = imageTooltip.addPara(
                                fixedName,
                                0f,
                            )
                            imageLabel.color = faction.baseUIColor
                            val oldPrev = textTwo.prev
                            val newText = textTwo.addImageWithText(0f)
                            if (!addedOneAlready) {
                                addedOneAlready = true
                            } else {
                                newText.position.rightOfMid(oldPrev, 0f)
                            }
                            if (beginningOfRow == null) {
                                beginningOfRow = newText
                            }
                            //imageLabel.position.setSize(imageLabel.computeTextWidth(fixedName), imageLabel.position.height)
                            val newWidth = (imageLabel.computeTextWidth(fixedName) + 25f).coerceAtLeast(150f)
                            if (newText.position.x + newText.position.width > panel.position.width) {
                                newText.position.belowMid(beginningOfRow, 10f)
                                beginningOfRow = newText
                            }
                            newText.position.setSize(newWidth, newText.position.height)
                            continue
                        }
                    }

                    panel.updateUIElementSizeAndMakeItProcessInput(textTwo)
                    //val boxTwo = panel.wrapTooltipWithBox(textTwo)
                    panel.addComponent(textTwo)
                    textTwo.position.belowLeft(box, 0f)
                }

                override fun getSourceMod(): ModSpecAPI? {
                    return augmentSpec.sourceMod
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
        createReciprocalLink(getAugmentEntryId("SA_fragmentSwarm"), CodexDataV2.getHullmodEntryId(HullMods.FRAGMENT_SWARM))
        createReciprocalLink(getAugmentEntryId("SA_fragmentSwarm"), CodexDataV2.getItemEntryId(Items.FRAGMENT_FABRICATOR))
        createReciprocalLink(getAugmentEntryId("SA_fragmentSwarm"), getAugmentEntryId("SA_secondaryFabricator"))
        createReciprocalLink(getAugmentEntryId("SA_fragmentSwarm"), getAugmentEntryId("SA_fragmentCoordinator"))

        createReciprocalLink(getAugmentEntryId("SA_secondaryFabricator"), CodexDataV2.getHullmodEntryId(HullMods.SECONDARY_FABRICATOR))
        createReciprocalLink(getAugmentEntryId("SA_secondaryFabricator"), CodexDataV2.getItemEntryId(Items.FRAGMENT_FABRICATOR))
        CodexDataV2.getEntry(getAugmentEntryId("SA_secondaryFabricator")).addRelatedEntry(getAugmentEntryId("SA_fragmentSwarm"))

        createReciprocalLink(getAugmentEntryId("SA_fragmentCoordinator"), CodexDataV2.getHullmodEntryId(HullMods.FRAGMENT_COORDINATOR))
        createReciprocalLink(getAugmentEntryId("SA_fragmentCoordinator"), CodexDataV2.getItemEntryId(Items.THREAT_PROCESSING_UNIT))
        CodexDataV2.getEntry(getAugmentEntryId("SA_fragmentCoordinator")).addRelatedEntry(getAugmentEntryId("SA_fragmentSwarm"))

        //createReciprocalLink(getAugmentEntryId("SA_constructionSwarms"), CodexDataV2.getShipEntryId("attack_swarm_Construction"))
        createReciprocalLink(getAugmentEntryId("SA_constructionSwarms"), CodexDataV2.getItemEntryId(Items.THREAT_PROCESSING_UNIT))
        CodexDataV2.getEntry(getAugmentEntryId("SA_constructionSwarms")).addRelatedEntry(getAugmentEntryId("SA_fragmentSwarm"))

        //createReciprocalLink(getAugmentEntryId("SA_attackSwarms"), CodexDataV2.getShipEntryId("attack_swarm_Attack"))
        createReciprocalLink(getAugmentEntryId("SA_attackSwarms"), CodexDataV2.getWeaponEntryId("swarm_launcher"))
        createReciprocalLink(getAugmentEntryId("SA_attackSwarms"), CodexDataV2.getItemEntryId(Items.THREAT_PROCESSING_UNIT))
        CodexDataV2.getEntry(getAugmentEntryId("SA_attackSwarms")).addRelatedEntry(getAugmentEntryId("SA_fragmentSwarm"))

        createReciprocalLink(getAugmentEntryId("SA_kineticFragments"), CodexDataV2.getWeaponEntryId("kinetic_fragments"))
        createReciprocalLink(getAugmentEntryId("SA_kineticFragments"), CodexDataV2.getItemEntryId(Items.THREAT_PROCESSING_UNIT))
        CodexDataV2.getEntry(getAugmentEntryId("SA_kineticFragments")).addRelatedEntry(getAugmentEntryId("SA_fragmentSwarm"))

        createReciprocalLink(getAugmentEntryId("SA_seekerFragments"), CodexDataV2.getWeaponEntryId("seeker_fragment"))
        createReciprocalLink(getAugmentEntryId("SA_seekerFragments"), CodexDataV2.getItemEntryId(Items.THREAT_PROCESSING_UNIT))
        CodexDataV2.getEntry(getAugmentEntryId("SA_seekerFragments")).addRelatedEntry(getAugmentEntryId("SA_fragmentSwarm"))

        createReciprocalLink(getAugmentEntryId("SA_defabSwarms"), CodexDataV2.getWeaponEntryId("devouring_swarm"))
        createReciprocalLink(getAugmentEntryId("SA_defabSwarms"), CodexDataV2.getItemEntryId(Items.THREAT_PROCESSING_UNIT))
        CodexDataV2.getEntry(getAugmentEntryId("SA_defabSwarms")).addRelatedEntry(getAugmentEntryId("SA_fragmentSwarm"))

        createReciprocalLink(getAugmentEntryId("SA_shroudedMantle"), CodexDataV2.getItemEntryId(Items.SHROUDED_MANTLE))
        createReciprocalLink(getAugmentEntryId("SA_shroudedLens"), CodexDataV2.getItemEntryId(Items.SHROUDED_LENS))
        createReciprocalLink(getAugmentEntryId("SA_shroudedThunderhead"), CodexDataV2.getItemEntryId(Items.SHROUDED_THUNDERHEAD))

    }

    private fun createReciprocalLink(entryIdOne: String, entryIdTwo: String) {
        val entryOne = CodexDataV2.getEntry(entryIdOne)
        val entryTwo = CodexDataV2.getEntry(entryIdTwo)

        entryOne.addRelatedEntry(entryTwo)
        entryTwo.addRelatedEntry(entryOne)
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