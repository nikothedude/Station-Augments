package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import niko_SA.SA_settings
import niko_SA.augments.core.stationAttachment

class solarShielding(market: MarketAPI, id: String) : stationAttachment(market, id) {

    companion object {
        const val CORONA_EFFECT_MULT = 0.2f
        const val ENERGY_DAMAGE_TAKEN_MULT = 0.8f
    }

    override val augmentCost: Float = 10f
        get() {
            if (SA_settings.MCTE_enabled) {
                return field * 2f // for obvious reasons
            }
            return field
        }
    override val name: String = "Stellar Shielding"
    override val spriteId: String = "graphics/icons/industry/mining.png"

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            station.mutableStats.energyDamageTakenMult.modifyMult(id, ENERGY_DAMAGE_TAKEN_MULT)
            station.mutableStats.energyShieldDamageTakenMult.modifyMult(id, ENERGY_DAMAGE_TAKEN_MULT)

            station.mutableStats.dynamic.getStat(Stats.CORONA_EFFECT_MULT).modifyMult(id, CORONA_EFFECT_MULT)
        }
    }
}