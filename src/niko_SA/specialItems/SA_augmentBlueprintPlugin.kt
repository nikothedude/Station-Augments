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
import niko_SA.augments.core.stationAugmentSpec
import niko_SA.augments.core.stationAugmentStore.allAugments
import niko_SA.augments.core.stationAugmentStore.getKnownAugments
import niko_SA.augments.core.stationAugmentStore.getPlayerKnownAugments
import niko_SA.augments.core.stationAugmentStore.teachAugment
import java.awt.Color

class SA_augmentBlueprintPlugin: BaseSpecialItemPlugin() {

    lateinit var augment: stationAttachment

    override fun init(stack: CargoStackAPI) {
        super.init(stack)

        val initialAugmentCheck = allAugments[stack.specialDataIfSpecial.data]
        if (initialAugmentCheck != null) {
            augment = initialAugmentCheck.getNewPluginInstance(null)
            return
        }

        val droppedFrom = spec.params // the drop group we were dropped from
        val picker = WeightedRandomPicker<Pair<String, stationAugmentSpec>>()
        //val newRandom = Random(Global.getSector().memoryWithoutUpdate[SA_ids.SA_nextAugmentBlueprintSeedMemId] as Long)
        //picker.random = newRandom
        //Global.getSector().memoryWithoutUpdate[SA_ids.SA_nextAugmentBlueprintSeedMemId] = newRandom.nextLong()
        for (entry in allAugments.entries) {
            val id = entry.key
            val data = entry.value

            var weight: Float = data.dropWeight
            if (Global.getSector().playerFaction.getKnownAugments().contains(id)) {
                weight *= 0.5f
            }
            if (weight > 0) {
                picker.add(Pair(id, data), weight)
            }
        }
        val augmentSet = picker.pick()
        if (augmentSet == null) {
            SA_debugUtils.log.error("null augment set when trying $droppedFrom! grabbing safety overrides to avoid a crash")
            augment = allAugments["SA_safetyOverrides"]!!.getNewPluginInstance(null)
        } else {
            augment = augmentSet.second.getNewPluginInstance(null)
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

        Global.getSettings().loadTexture(augment.getImageName())
        val sprite = Global.getSettings().getSprite(augment.getImageName())
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
        return ("${augment.getName()} - Station Augment")
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
        val known = Global.getSector().playerFaction.getKnownAugments().contains(industryId)
        augment.gettingDescFromBlueprint = true
        augment.getBasicDescription(tooltip, expanded, null)
        augment.gettingDescFromBlueprint = false
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
                "" + augment.getName() + ": blueprint already known"
            ) //,
        } else {
            Global.getSoundPlayer().playUISound("ui_acquired_blueprint", 1f, 1f)
            Global.getSector().playerFaction.teachAugment(augment.id)
            Global.getSector().campaignUI.messageDisplay.addMessage(
                "Acquired blueprint: " + augment.getName() + ""
            ) //,
        }
    }

    override fun getDesignType(): String {
        return augment.getSpec().manufacturer
    }
}