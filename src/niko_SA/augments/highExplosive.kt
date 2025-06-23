package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent

class highExplosive() : stationAttachment() {

    companion object {
        const val FLUX_CAPACITY_INCREMENT = 3000f

        const val EXPLOSION_DAMAGE_MULT = 9f
        const val EXPLOSION_RADIUS_MULT = 7f
    }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            station.mutableStats.fluxCapacity.modifyFlat(id, FLUX_CAPACITY_INCREMENT)
        }

        station.mutableStats.dynamic.getStat(Stats.EXPLOSION_DAMAGE_MULT).modifyFlat(id, EXPLOSION_DAMAGE_MULT)
        station.mutableStats.dynamic.getStat(Stats.EXPLOSION_RADIUS_MULT).modifyFlat(id, EXPLOSION_RADIUS_MULT)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Increases explosion radius and damage of the station core by %s and %s respectively." +
                    "" +
                    "Also increases flux capacity of all modules by %s.",
            5f,
            Misc.getHighlightColor(),
            toPercent(EXPLOSION_RADIUS_MULT), toPercent(EXPLOSION_DAMAGE_MULT), "${FLUX_CAPACITY_INCREMENT.toInt()}"
        )
    }

    override fun getBlueprintValue(): Int {
        return 2000
    }
}