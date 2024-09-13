package niko_SA.augments.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomDialogDelegate
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomDialogDelegate.CustomDialogCallback
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.intel.events.BaseFactorTooltip
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.DialogUtils.getChildrenCopy
import niko_SA.MarketUtils.getRemainingAugmentBudget
import niko_SA.MarketUtils.getStationAugments
import niko_SA.MarketUtils.getUsedAugmentBudget
import niko_SA.MarketUtils.toggleStationAugment
import niko_SA.SA_settings.ALLOW_MODIFY_OF_ALL_STATIONS
import niko_SA.augments.core.stationAugmentStore.allAugments
import niko_SA.augments.core.stationAugmentStore.getPlayerKnownAugments
import java.awt.Color
import kotlin.math.roundToInt

// all this has to do is show the existing augments, not elegant but it works
class AugmentMenuDialogueDelegate(val station: Industry): BaseCustomDialogDelegate() {
    companion object {
        val HEIGHT = (Global.getSettings().screenHeight - 400.0f)
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
    val mode: Mode = if (ALLOW_MODIFY_OF_ALL_STATIONS || market.isPlayerOwned)  Mode.MODIFYING else Mode.VISITING

    var basePanel: CustomPanelAPI? = null
    var panel: CustomPanelAPI? = null

    // mostly taken from indevo's ChangelingIndustryDialogueDelegate
    override fun createCustomDialog(panel: CustomPanelAPI?, callback: CustomDialogCallback?) {
        if (panel == null || callback == null) return
        basePanel = panel
        regenerateDialog(callback)
    }

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

        val panelTooltip = panel!!.createUIElement(WIDTH, HEIGHT, true)
        val sectionHeading = if (mode == Mode.MODIFYING) "Known/Installed augments" else "Currently installed augments"
        panelTooltip.addSectionHeading(sectionHeading, Alignment.MID, 0.0f)

        buttons.clear()
        val opad = 10.0f
        val spad = 2.0f

        val installedAugments = market.getStationAugments()
        val augmentsToShow = HashMap<String, stationAugmentData>()
        installedAugments.forEach { augmentsToShow[it.id] = allAugments[it.id]!! }
        if (mode == Mode.MODIFYING) {
            getPlayerKnownAugments().forEach { augmentsToShow[it] = allAugments[it]!! }
        }

        for (augmentEntry in augmentsToShow.toSortedMap()) {
            val augmentData = augmentEntry.value
            val augmentId = augmentEntry.key

            val preExistingAugment = (installedAugments.firstOrNull { it.id == augmentId })
            val augmentInstance = preExistingAugment ?: augmentData.getInstance(market)

            if (augmentInstance.applied) {
                augmentInstance.considerAP = false
            }
            val canBuild = augmentInstance.canBeModifiedOrBuilt()
            augmentInstance.considerAP = true
            val canAfford = augmentInstance.canAfford()

            var baseColor = Misc.getButtonTextColor()
            var bgColour = Misc.getDarkPlayerColor()
            var brightColor = Misc.getBrightPlayerColor()
            if (!canAfford) {
                baseColor = Color.darkGray
                bgColour = Color.lightGray
                brightColor = Color.gray
            }

            val augmentButtonPanel = panel!!.createCustomPanel(
                595.0f,
                86.0f,
                ButtonReportingCustomPanel(this, callback)
            )
            val spriteName: String = augmentInstance.getImageName(market)
            Global.getSettings().loadTexture(spriteName)
            val sprite = Global.getSettings().getSprite(spriteName)
            val aspectRatio = sprite.width / sprite.height
            val adjustedWidth = (80.0f * aspectRatio).coerceAtMost(sprite.width)
            val defaultPadding = 2.0f
            val textPanel: TooltipMakerAPI =
                augmentButtonPanel.createUIElement(595.0f - adjustedWidth - opad - defaultPadding, 80.0f, false)

            if (mode == Mode.MODIFYING && (canBuild && canAfford)) {
                textPanel.addSectionHeading(" " + augmentInstance.name, Alignment.LMID, 0.0f)
            } else {
                textPanel.addSectionHeading(" " + augmentInstance.name, Color.WHITE, Misc.getGrayColor(), Alignment.LMID, 0.0f)
            }

            val anonymousTooltip = object : BaseFactorTooltip() {
                override fun createTooltip(tooltip: TooltipMakerAPI, expanded: Boolean, tooltipParam: Any) {
                    augmentInstance.getBasicDescription(tooltip, expanded)
                }
            }
            textPanel.addTooltipTo(anonymousTooltip, textPanel, TooltipMakerAPI.TooltipLocation.LEFT)
            //textPanel.addTooltipToPrevious(anonymousTooltip, TooltipMakerAPI.TooltipLocation.LEFT, false)
           // augmentInstance.getBasicDescription(textPanel, false)
            val cost = augmentInstance.augmentCost
            val color = if (cost <= market.getRemainingAugmentBudget()) Misc.getHighlightColor() else Misc.getNegativeHighlightColor()
            textPanel.addPara(
                "%s AP",
                5f,
                color,
                "$cost"
            )
            val unavailableReason = augmentInstance.getUnavailableReason()
            if (unavailableReason != null) {
                textPanel.addPara(unavailableReason, opad, Misc.getNegativeHighlightColor(), unavailableReason)
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
            areaCheckbox.isEnabled = (mode == Mode.MODIFYING && (canAfford && canBuild))

            augmentButtonPanel.addUIElement(anchor).inTL(-opad, 0.0f)
            anchor = augmentButtonPanel.createUIElement(adjustedWidth, 84.0f, false)
            anchor.addImage(spriteName, adjustedWidth, 80.0f.coerceAtMost(sprite.height), 0.0f)
            augmentButtonPanel.addUIElement(anchor).inTL(defaultPadding - opad, defaultPadding)
            augmentButtonPanel.addUIElement(textPanel).rightOfTop(anchor, opad)

            panelTooltip.addCustom(augmentButtonPanel, 0.0f)
            buttons.add(areaCheckbox)

        }
        basePanel!!.addComponent(panel!!)
        panel!!.addUIElement(panelTooltip).inTMid(0f)
    }

    fun reportButtonPressed(buttonId: Any) {
        if (buttonId !is stationAttachment) return
        market.toggleStationAugment(buttonId, true)
    }

}