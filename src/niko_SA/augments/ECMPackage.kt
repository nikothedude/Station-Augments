package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class ECMPackage() : stationAttachment() {

    companion object {
        const val ECM_VALUE = 15f
        const val DEFENSE_MULT = 1.05f
    }

    override fun applyInCombat(station: ShipAPI) {
        station.mutableStats.dynamic.getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, ECM_VALUE)
    }

    override fun apply() {
        super.apply()

        market?.stats?.dynamic?.getMod(Stats.GROUND_DEFENSES_MOD)?.modifyMult(id, DEFENSE_MULT)
    }

    override fun unapply() {
        market?.stats?.dynamic?.getMod(Stats.GROUND_DEFENSES_MOD)?.unmodify(id)
    }

    override fun getUnavailableReason(): String? {
        if (getStationIndustry()?.isDisrupted == true) {
            return "Cannot be added to a disrupted station"
        }

        return super.getUnavailableReason()
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "Increases in-combat ECM value by %s.",
            5f,
            Misc.getHighlightColor(),
            "${ECM_VALUE.toInt()}%"
        )
        tooltip.addPara(
            "Increases market defense rating by %s.",
            5f,
            Misc.getHighlightColor(),
            "${DEFENSE_MULT}x"
        )

        val industry = getStationIndustry()
        if (!applied && industry?.isDisrupted == true) {
            tooltip.addPara(
                "The ${industry.currentName} is currently disrupted, meaning this augment %s until it is repaired.",
                5f,
                Misc.getNegativeHighlightColor(),
                "cannot be applied"
            ).setColor(Misc.getGrayColor())
        } else {
            tooltip.addPara(
                "Cannot be applied if the station is currently under repairs.",
                5f
            ).setColor(Misc.getGrayColor())
        }

        tooltip.addPara(
            "The defense rating increase persists even if the station is disrupted.",
            5f
        ).setColor(Misc.getGrayColor())
    }
}