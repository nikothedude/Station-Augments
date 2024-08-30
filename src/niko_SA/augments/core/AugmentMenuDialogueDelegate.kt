package niko_SA.augments.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.BaseCustomDialogDelegate
import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.CustomDialogDelegate
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.ButtonAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.getStationAugments
import niko_SA.MarketUtils.toggleStationAugment
import java.awt.Color

// all this has to do is show the existing augments, not elegant but it works
class AugmentMenuDialogueDelegate(val station: Industry): BaseCustomDialogDelegate() {

    companion object {
        val HEIGHT = (Global.getSettings().screenHeight - 300.0f)
        const val WIDTH = 600f

        class ButtonReportingCustomPanel(var delegate: AugmentMenuDialogueDelegate) :
            BaseCustomUIPanelPlugin() {
            override fun buttonPressed(buttonId: Any) {
                super.buttonPressed(buttonId)
                delegate.reportButtonPressed(buttonId)
            }
        }
    }

    enum class Mode {
        MODIFYING,
        VISITING // cant interact with the market, so we cant do anything
    }

    var buttons: MutableList<ButtonAPI> = ArrayList()
    val market: MarketAPI = station.market!! // !! not necessary, but good for explicitness
    val mode: Mode = if (market.isPlayerOwned)  Mode.MODIFYING else Mode.VISITING

    // mostly taken from indevo's ChangelingIndustryDialogueDelegate
    override fun createCustomDialog(panel: CustomPanelAPI?, callback: CustomDialogDelegate.CustomDialogCallback?) {
        if (panel == null) return
        val panelTooltip = panel.createUIElement(WIDTH, HEIGHT, true)
        panelTooltip.addSectionHeading("Currently installed augments", Alignment.MID, 0.0f)

        buttons.clear()
        val opad = 10.0f
        val spad = 2.0f

        val installedAugments = market.getStationAugments()

        for (augmentEntry in stationAugmentStore.allAugments) {
            val augmentData = augmentEntry.value
            val augmentId = augmentEntry.key

            val preExistingAugment = (installedAugments.firstOrNull { it.id == augmentId })
            val augmentInstance = preExistingAugment ?: augmentData.getInstance(market)

            val canBuild = augmentInstance.canBeBuilt()
            val canAfford = augmentInstance.canAfford()

            var baseColor = Misc.getButtonTextColor()
            var bgColour = Misc.getDarkPlayerColor()
            var brightColor = Misc.getBrightPlayerColor()
            if (!canAfford) {
                baseColor = Color.darkGray
                bgColour = Color.lightGray
                brightColor = Color.gray
            }

            val augmentButtonPanel = panel.createCustomPanel(
                595.0f,
                86.0f,
                ButtonReportingCustomPanel(this)
            )
            val spriteName: String = augmentInstance.getImageName(market)
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

            augmentInstance.createTooltip(panelTooltip, false)
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

            val baseHeight = textPanel.heightSoFar + 2.0f + opad
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

    }


    fun reportButtonPressed(buttonId: Any) {
        if (buttonId !is stationAttachment) return
        market.toggleStationAugment(buttonId)
    }

}