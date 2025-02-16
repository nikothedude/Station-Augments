package niko_SA.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.SectorMapAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

class SA_TTBlackSiteBreadcrumbIntel(
    val approximateLocation: SectorEntityToken
): BaseIntelPlugin() {

    override fun getName(): String = "A strange tip-off"
    override fun getIntelTags(map: SectorMapAPI?): MutableSet<String> {
        return (super.getIntelTags(map) + mutableSetOf(Tags.INTEL_EXPLORATION)).toMutableSet()
    }

    override fun getIcon(): String {
        return Global.getSettings().getSpriteName("intel", "comm_sniffer")
    }

    override fun createSmallDescription(info: TooltipMakerAPI?, width: Float, height: Float) {
        if (info == null) return

        info.addPara(
            "You were passed a %s holding %s, and a request to \"%s\" - with an additional promise for %s.",
            5f,
            Misc.getHighlightColor(),
            "data chit", "hyperspace coordinates", "fix a mistake", "salvage"
        )

        info.addPara(
            "No more information was given - this could certainly be a %s, but if it isn't, who knows what you might find.",
            5f,
            Misc.getNegativeHighlightColor(),
            "trap"
        )
    }

    override fun getMapLocation(map: SectorMapAPI?): SectorEntityToken {
        return approximateLocation
    }
}