package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_settings
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent

class solarShielding(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    companion object {
        const val CORONA_EFFECT_MULT = 0.2f
        const val ENERGY_DAMAGE_TAKEN_MULT = 0.8f
    }

    override val manufacturer: String = "Sindrian Diktat"
    override val augmentCost: Float = 10f
        get() {
            if (SA_settings.MCTE_enabled) {
                return field * 2f // for obvious reasons
            }
            return field
        }
    override val name: String = "Stellar Shielding"
    override val spriteId: String = "graphics/hullmods/solar_shielding2.png"

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            station.mutableStats.energyDamageTakenMult.modifyMult(id, ENERGY_DAMAGE_TAKEN_MULT)
            station.mutableStats.energyShieldDamageTakenMult.modifyMult(id, ENERGY_DAMAGE_TAKEN_MULT)

            station.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifyMult(id, CORONA_EFFECT_MULT)
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "A few cycles after the advent of solar shielding, sindrian engineers standardized the design into two parts: Orbital, and Spacefaring. " +
            "While significantly harder to star-proof a station (more than they already are), it IS possible, and even offers greater rewards.",
            5f
        )

        val para = tooltip.addPara(
            "Decreases the effect operating in a star's corona has on the station by %s, as well as "  +
            "reducing energy damage taken by %s.",
            5f,
            Misc.getHighlightColor(),
            toPercent(1 - CORONA_EFFECT_MULT), toPercent(1 - ENERGY_DAMAGE_TAKEN_MULT)
        )
    }
}