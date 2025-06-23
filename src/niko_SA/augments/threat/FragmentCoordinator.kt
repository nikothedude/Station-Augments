package niko_SA.augments.threat

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.combat.threat.FragmentCoordinatorHullmod
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc

class FragmentCoordinator: ThreatAugment() {

    companion object {
        const val SIZE_INCREASE = 40f
    }

    override fun applyInCombat(station: ShipAPI) {
        station.mutableStats.dynamic.getMod(Stats.FRAGMENT_SWARM_SIZE_MOD).modifyPercent(id, SIZE_INCREASE)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Increases the size of the station's fragment swarm by %s.",
            5f,
            Misc.getHighlightColor(),
            "${SIZE_INCREASE.toInt()}%"
        )
    }
}