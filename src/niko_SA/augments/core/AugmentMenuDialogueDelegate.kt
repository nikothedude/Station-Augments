package niko_SA.augments.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomDialogDelegate
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomDialogDelegate.CustomDialogCallback
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.ScrollPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.DialogUtils.getChildrenCopy
import niko_SA.MarketUtils.getAugmentBudget
import niko_SA.MarketUtils.getRemainingAugmentBudget
import niko_SA.MarketUtils.getStationAugments
import niko_SA.MarketUtils.getStationIndustry
import niko_SA.MarketUtils.getUsedAugmentBudget
import niko_SA.MarketUtils.toggleStationAugment
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.SA_settings.ALLOW_MODIFY_OF_ALL_STATIONS
import niko_SA.augments.core.stationAugmentStore.allAugments
import niko_SA.augments.core.stationAugmentStore.getPlayerKnownAugments
import niko_SA.codex.CodexData
import java.awt.Color
import kotlin.math.abs

// all this has to do is show the existing augments, not elegant but it works
class AugmentMenuDialogueDelegate(val station: Industry): BaseCustomDialogDelegate() {
    companion object {
        val HEIGHT = (Global.getSettings().screenHeight - 300.0f)
        const val WIDTH = 600f

        class ButtonReportingCustomPanel(var delegate: AugmentMenuDialogueDelegate, val callback: CustomDialogCallback) :
            BaseCustomUIPanelPlugin() {
            override fun buttonPressed(buttonId: Any) {
                super.buttonPressed(buttonId)
                delegate.reportButtonPressed(buttonId)
                delegate.regenerateDialog(callback)
            }
        }
    }

    enum class Mode {
        MODIFYING,
        VISITING // cant interact with the market, so we cant do anything
    }

    var buttons: MutableList<ButtonAPI> = ArrayList()
    val market: MarketAPI = station.market!! // !! not necessary, but good for explicitness
    val mode: Mode = if (ALLOW_MODIFY_OF_ALL_STATIONS || market.isPlayerOwned) Mode.MODIFYING else Mode.VISITING

    var basePanel: CustomPanelAPI? = null
    var panel: CustomPanelAPI? = null
    var scroller: ScrollPanelAPI? = null

    val sideTooltip: TooltipMakerAPI? = null

    val cachedAppliedStatus = HashMap<String, Boolean>()

    // mostly taken from indevo's ChangelingIndustryDialogueDelegate
    override fun createCustomDialog(panel: CustomPanelAPI?, callback: CustomDialogCallback?) {
        if (panel == null || callback == null) return
        basePanel = panel

        val installedAugments = market.getStationAugments()
        val augmentsToShow = getAugmentsToShow()
        for (augment in augmentsToShow) {
            val id = augment.key

            cachedAppliedStatus[id] = (installedAugments.firstOrNull { it.id == id }?.applied) == true
        }

        regenerateDialog(callback)
        this.callback = callback
    }
    var callback: CustomDialogCallback? = null

    fun regenerateDialog(callback: CustomDialogCallback) {
        val oldPanel = panel
        if (oldPanel != null) {
            for (entry in oldPanel.getChildrenCopy()) {
                oldPanel.removeComponent(entry)
            }
            basePanel!!.removeComponent(oldPanel)
        }
        // this panel code is taken from indevo's petmanagerdelegatecode, we want stuff to updaet when the button is pressed
        panel = Global.getSettings().createCustom(basePanel!!.position.width, basePanel!!.position.height, null)
        /*val secondPanel = Global.getSettings().createCustom(basePanel!!.position.width * 0.8f, basePanel!!.position.height * 0.4f, null)
        val contextTooltip = secondPanel.createUIElement(secondPanel!!.position.width, secondPanel.position.height, false)
        contextTooltip.addPara(
            "test", 5f
        )
        secondPanel.addUIElement(contextTooltip).aboveLeft(panel, 0f)*/

        val APstring = "(AP remaining: ${market.getRemainingAugmentBudget().trimHangingZero()})"
        val panelTooltip = panel!!.createUIElement(WIDTH, HEIGHT, true)
        val sectionHeading = if (mode == Mode.MODIFYING) "Known/Installed augments $APstring" else "Currently installed augments $APstring"
        panelTooltip.addSectionHeading(sectionHeading, Alignment.MID, 0.0f)

        buttons.clear()
        val opad = 10.0f

        val installedAugments = market.getStationAugments()
        val augmentsToShow = getAugmentsToShow()

        if (augmentsToShow.isEmpty()) {
            var nothingString = if (mode == Mode.MODIFYING) "No augments known or installed" else "No augments installed"
            panelTooltip.setParaFontOrbitron()
            val para = panelTooltip.addPara(
                nothingString, 30f
            )
            para.setAlignment(Alignment.MID)
            para.color = Misc.getButtonTextColor()
            panelTooltip.setParaFontDefault()
        } else {

            class AugmentSortingComparator(): Comparator<String> {
                override fun compare(augmentIdOne: String, augmentIdTwo: String): Int {
                    val augmentSpecOne = augmentsToShow[augmentIdOne]!!
                    val augmentSpecTwo = augmentsToShow[augmentIdTwo]!!

                    val augmentInstanceOne = (installedAugments.firstOrNull { it.id == augmentIdOne }) ?: augmentSpecOne.getNewPluginInstance(market)
                    val augmentInstanceTwo = (installedAugments.firstOrNull { it.id == augmentIdTwo }) ?: augmentSpecTwo.getNewPluginInstance(market)

                    // cache applied status once the ui opens and dont move it
                    // job for a hashmap?
                    val appliedOne = cachedAppliedStatus[augmentIdOne] == true
                    val appliedTwo = cachedAppliedStatus[augmentIdTwo] == true

                    if (appliedOne && !appliedTwo) return -1
                    if (!appliedOne && appliedTwo) return 1
                    //if (augmentInstanceOne?.applied == augmentInstanceTwo?.applied) return 0 // in the case its the same, we sort alphabetically

                    return augmentInstanceOne.sortInUIAgainst(augmentInstanceTwo)
                }
            }

            for (augmentEntry in augmentsToShow.toSortedMap(AugmentSortingComparator())) {
                val augmentData = augmentEntry.value
                val augmentId = augmentEntry.key

                val preExistingAugment = (installedAugments.firstOrNull { it.id == augmentId })
                val augmentInstance = preExistingAugment ?: augmentData.getNewPluginInstance(market)

                if (augmentInstance.applied) {
                    augmentInstance.considerAP = false
                }
                val canBuild = augmentInstance.canBeModifiedOrBuilt()
                augmentInstance.considerAP = true
                val canAfford = augmentInstance.canAfford()
                val canRemove = augmentInstance.canBeRemoved()

                var baseColor = Misc.getButtonTextColor()
                var bgColour = Misc.getDarkPlayerColor()
                var brightColor = Misc.getBrightPlayerColor()
                if (!canAfford) {
                    baseColor = Color.darkGray
                    bgColour = Color.lightGray
                    brightColor = Color.gray
                }

                val augmentButtonPanel = panel!!.createCustomPanel(
                    augmentInstance.getIdealButtonWidth(panel),
                    augmentInstance.getIdealButtonHeight(panel),
                    ButtonReportingCustomPanel(this, callback)
                )
                val spriteName: String = augmentInstance.getImageName(market)
                Global.getSettings().loadTexture(spriteName)
                val sprite = Global.getSettings().getSprite(spriteName)
                val aspectRatio = sprite.width / sprite.height
                val adjustedWidth = (80.0f * aspectRatio).coerceAtMost(sprite.width)
                val defaultPadding = 2.0f
                val textPanel: TooltipMakerAPI = augmentButtonPanel.createUIElement(augmentInstance.getIdealButtonWidth(panel) - adjustedWidth - opad - defaultPadding, augmentInstance.getIdealButtonHeight(panel), false)

                if (mode == Mode.MODIFYING && (canBuild && canAfford)) {
                    textPanel.addSectionHeading(" " + augmentInstance.getName(), augmentInstance.getNameColor(), Global.getSector().playerFaction.darkUIColor, Alignment.LMID, 0.0f)
                } else {
                    textPanel.addSectionHeading(
                        " " + augmentInstance.getName(),
                        augmentInstance.getNameColor(),
                        Misc.getGrayColor(),
                        Alignment.LMID,
                        0.0f
                    )
                }

                val anonymousTooltip = object : BaseFactorTooltip() {
                    override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                        if (panel != null) {
                            augmentInstance.getBasicDescription(tooltip, expanded, panel)
                        }
                    }
                }
                textPanel.addTooltipTo(anonymousTooltip, textPanel, TooltipMakerAPI.TooltipLocation.LEFT)
                //textPanel.addTooltipToPrevious(anonymousTooltip, TooltipMakerAPI.TooltipLocation.LEFT, false)
                // augmentInstance.getBasicDescription(textPanel, false)
                val cost = augmentInstance.getAugmentCost()
                var APColor = if (augmentInstance.applied || cost <= market.getRemainingAugmentBudget()) Misc.getHighlightColor() else Misc.getNegativeHighlightColor()
                if (cost < 0f) APColor = Misc.getPositiveHighlightColor()
                val builtInColor = augmentInstance.builtInMode.getAPColor()
                if (builtInColor != null) {
                    APColor = builtInColor
                }
                val plusOrNot = if (cost >= 0f) "" else "+"
                textPanel.addPara(
                    "%s AP",
                    5f,
                    APColor,
                    "$plusOrNot${abs(cost).trimHangingZero()}"
                )
                if (!augmentInstance.applied) {
                    val unavailableReason = augmentInstance.getUnavailableReason()
                    if (unavailableReason != null) {
                        textPanel.addPara(unavailableReason, opad, Misc.getNegativeHighlightColor(), unavailableReason)
                    }
                } else {
                    augmentInstance.modifyAugmentMenu(textPanel, panel, augmentButtonPanel, this)
                }
                /*textPanel.addPara(augmentInstance.getDescription().getText2(), opad)
            if (!canBuild) {
                textPanel.addPara(augmentInstance.getUnavailableReason(), Misc.getNegativeHighlightColor(), spad)
                    .setAlignment(Alignment.RMID)
            } else {
                textPanel.addPara(
                    "Credit cost: %s",
                    spad,
                    if (canAfford) Misc.getPositiveHighlightColor() else Misc.getNegativeHighlightColor(),
                    *arrayOf<String>(Misc.getDGSCredits(augmentInstance.creditCost))
                ).setAlignment(
                    Alignment.RMID
                )
            }*/

                val baseHeight = /*textPanel.heightSoFar + */80f.coerceAtLeast(sprite.height)// + opad
                augmentButtonPanel.position.setSize(595.0f, 84.0f.coerceAtLeast(baseHeight))
                var anchor: TooltipMakerAPI = augmentButtonPanel.createUIElement(595.0f, baseHeight, false)
                val areaCheckbox = anchor.addAreaCheckbox(
                    "",
                    augmentInstance,
                    baseColor,
                    bgColour,
                    brightColor,
                    595.0f,
                    baseHeight,
                    0.0f,
                    true
                )
                areaCheckbox.isChecked = augmentInstance.applied
                areaCheckbox.isEnabled = false
                if (mode == Mode.MODIFYING) {
                    if (augmentInstance.applied) {
                        if (canRemove) {
                            areaCheckbox.isEnabled = true
                        }
                    } else {
                        if (canAfford && canBuild) {
                            areaCheckbox.isEnabled = true
                        }
                    }
                }

                augmentButtonPanel.addUIElement(anchor).inTL(-opad, 0.0f)
                anchor = augmentButtonPanel.createUIElement(adjustedWidth, 84.0f, false)
                anchor.addImage(spriteName, adjustedWidth, 80.0f.coerceAtMost(sprite.height), 0.0f)
                augmentButtonPanel.addUIElement(anchor).inTL(defaultPadding - opad, defaultPadding)
                augmentButtonPanel.addUIElement(textPanel).rightOfTop(anchor, opad)

                panelTooltip.addCustom(augmentButtonPanel, 0.0f)
                buttons.add(areaCheckbox)

            }
        }
        basePanel!!.addComponent(panel!!)
        panel!!.addUIElement(panelTooltip).inTMid(0f)

        val oldXOffset = scroller?.xOffset ?: 0f
        val oldYOffset = scroller?.yOffset ?: 0f

        scroller = panelTooltip.externalScroller

        panelTooltip.externalScroller?.xOffset = oldXOffset
        panelTooltip.externalScroller?.yOffset = oldYOffset
    }

    private fun getAugmentsToShow(): HashMap<String, stationAugmentSpec> {
        val installedAugments = market.getStationAugments()
        val augmentsToShow = HashMap<String, stationAugmentSpec>()
        installedAugments.forEach { augmentsToShow[it.id] = allAugments[it.id]!! }
        installedAugments.forEach { CodexData.unlockAugment(it.id) }
        if (mode == Mode.MODIFYING) {
            getPlayerKnownAugments().forEach { augmentsToShow[it] = allAugments[it]!! }
        }
        return augmentsToShow
    }

    fun reportButtonPressed(buttonId: Any) {
        if (buttonId !is stationAttachment) return
        market.toggleStationAugment(buttonId, true)
    }

}