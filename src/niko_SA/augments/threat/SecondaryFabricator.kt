package niko_SA.augments.threat

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

class SecondaryFabricator: ThreatAugment() {

    companion object {
        const val RESPAWN_PERCENT = 60f
    }

    override fun applyInCombat(station: ShipAPI) {
        station.mutableStats.dynamic.getStat(Stats.FRAGMENT_SWARM_RESPAWN_RATE_MULT).modifyPercent(id, RESPAWN_PERCENT)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Increases the rate at which replacement fragments are launched by %s.",
            0f,
            Misc.getHighlightColor(),
            "${RESPAWN_PERCENT.toInt()}%"
        )
    }
}