package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class ECMPackage(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    companion object {
        const val ECM_VALUE = 15f
        const val DEFENSE_MULT = 1.05f
    }

    override val augmentCost: Float = 9f
    override val name: String = "ECM Package"
    override val spriteId: String = "graphics/hullmods/ecm_package.png"

    override fun applyInCombat(station: ShipAPI) {
        station.mutableStats.dynamic.getMod(Stats.ELECTRONIC_WARFARE_FLAT).modifyFlat(id, ECM_VALUE)
    }

    override fun apply() {
        super.apply()

        val stationIndustry = getStationIndustry() ?: return
        if (stationIndustry.isFunctional) {
            market?.stats?.dynamic?.getMod(Stats.GROUND_DEFENSES_MOD)?.modifyMult(id, DEFENSE_MULT)
        }
    }

    override fun unapply() {
        market?.stats?.dynamic?.getMod(Stats.GROUND_DEFENSES_MOD)?.unmodify(id)
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
    }
}