package niko_SA.specialItems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CargoStackAPI
import com.fs.starfarer.api.campaign.CargoTransferHandlerAPI
import com.fs.starfarer.api.campaign.SpecialItemPlugin.SpecialItemRendererAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.econ.SubmarketAPI
import com.fs.starfarer.api.campaign.impl.items.BaseSpecialItemPlugin
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import niko_SA.SA_debugUtils
import niko_SA.augments.core.stationAttachment
import niko_SA.augments.core.stationAugmentData
import niko_SA.augments.core.stationAugmentStore.allAugments
import niko_SA.augments.core.stationAugmentStore.getKnownAugments
import niko_SA.augments.core.stationAugmentStore.getPlayerKnownAugments
import java.awt.Color

class SA_augmentBlueprintPlugin: BaseSpecialItemPlugin() {

    lateinit var augment: stationAttachment

    override fun init(stack: CargoStackAPI) {
        super.init(stack)

        val initialAugmentCheck = allAugments[stack.specialDataIfSpecial.data]
        if (initialAugmentCheck != null) {
            augment = initialAugmentCheck.getInstance(null)
            return
        }

        val droppedFrom = spec.params // the drop group we were dropped from
        val picker = WeightedRandomPicker<Pair<String, stationAugmentData>>()
        for (entry in allAugments.entries) {
            val id = entry.key
            val data = entry.value

            val weight: Float? = data.dropGroupWeights[droppedFrom]
            if (weight != null) {
                picker.add(Pair(id, data), weight)
            }
        }
        val augmentSet = picker.pick()
        if (augmentSet == null) {
            SA_debugUtils.log.error("null augment set when trying $droppedFrom! grabbing safety overrides to avoid a crash")
            augment = allAugments["SA_safetyOverrides"]!!.getInstance(null)
        } else {
            augment = augmentSet.second.getInstance(null)
        }
        stack.specialDataIfSpecial.data = augment.id

        //augment = allAugments[stack.specialDataIfSpecial.data]!!.getInstance(null) // this will crash if you enter a invalid thing, but thats ok
    }

    override fun render(
        x: Float, y: Float, w: Float, h: Float, alphaMult: Float,
        glowMult: Float, renderer: SpecialItemRendererAPI
    ) {
        val cx = x + w / 2f
        val cy = y + h / 2f

        val blX = cx - 31f
        val blY = cy - 16f
        val tlX = cx - 22f
        val tlY = cy + 27f
        val trX = cx + 23f
        val trY = cy + 27f
        val brX = cx + 15f
        val brY = cy - 19f

        val sprite = Global.getSettings().getSprite(augment.spriteId)
        val known = getPlayerKnownAugments().contains(augment.id)

        val mult = 1f
        sprite.alphaMult = alphaMult * mult
        sprite.setNormalBlend()
        sprite.renderWithCorners(blX, blY, tlX, tlY, trX, trY, brX, brY)
        if (glowMult > 0) {
            sprite.alphaMult = alphaMult * glowMult * 0.5f * mult
            sprite.setAdditiveBlend()
            sprite.renderWithCorners(blX, blY, tlX, tlY, trX, trY, brX, brY)
        }
        if (known) {
            renderer.renderBGWithCorners(
                Color.black, blX, blY, tlX, tlY, trX, trY, brX, brY,
                alphaMult * 0.5f, 0f, false
            )
        }
        renderer.renderScanlinesWithCorners(blX, blY, tlX, tlY, trX, trY, brX, brY, alphaMult, false)
    }

    override fun getPrice(market: MarketAPI?, submarket: SubmarketAPI?): Int {
        return augment.getBlueprintValue()
    }

    override fun getName(): String {
        return ("Station augment: ${augment.name}")
    }

    override fun createTooltip(
        tooltip: TooltipMakerAPI,
        expanded: Boolean,
        transferHandler: CargoTransferHandlerAPI?,
        stackSource: Any?
    ) {
        super.createTooltip(tooltip, expanded, transferHandler, stackSource)
        val pad = 3f
        val opad = 10f
        val small = 5f
        val h = Misc.getHighlightColor()
        val g = Misc.getGrayColor()
        var b = Misc.getButtonTextColor()
        b = Misc.getPositiveHighlightColor()
        val industryId: String = stack.specialDataIfSpecial.data
        val known = Global.getSector().playerFaction.knowsIndustry(industryId)
        augment.getBasicDescription(tooltip, expanded)
        addCostLabel(tooltip, opad, transferHandler, stackSource)
        if (known) {
            tooltip.addPara("Already known", g, opad)
        } else {
            tooltip.addPara("Right-click to learn", b, opad)
        }
    }

    override fun hasRightClickAction(): Boolean {
        return true
    }

    override fun shouldRemoveOnRightClickAction(): Boolean {
        return !getPlayerKnownAugments().contains(augment.id)
    }

    override fun performRightClickAction() {
        if (Global.getSector().playerFaction.getKnownAugments().contains(augment.id)) {
            Global.getSector().campaignUI.messageDisplay.addMessage(
                "" + augment.name + ": blueprint already known"
            ) //,
        } else {
            Global.getSoundPlayer().playUISound("ui_acquired_blueprint", 1f, 1f)
            Global.getSector().playerFaction.getKnownAugments() += augment.id
            Global.getSector().campaignUI.messageDisplay.addMessage(
                "Acquired blueprint: " + augment.name + ""
            ) //,
        }
    }

    override fun getDesignType(): String {
        return augment.manufacturer
    }

    override fun addCostLabel(
        tooltip: TooltipMakerAPI?,
        pad: Float,
        transferHandler: CargoTransferHandlerAPI?,
        stackSource: Any?
    ) {
        super.addCostLabel(tooltip, pad, transferHandler, stackSource)
    }
}