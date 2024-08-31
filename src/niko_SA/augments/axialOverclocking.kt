package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import niko_SA.augments.core.stationAttachment

class axialOverclocking(market: MarketAPI, id: String) : stationAttachment(market, id) {
    companion object {
        const val TURN_MULT = 6f

        /** Otherwise, these stations cant actually turn their guns fast enough to aim at anything. */
        val stationTypesToGetExtraWeaponTurnrate: HashSet<String> = hashSetOf(Industries.ORBITALSTATION_MID, Industries.BATTLESTATION_MID, Industries.STARFORTRESS_MID)
        const val WEAPON_TURNRATE_MULT = 5f
    }

    override val augmentCost: Float = 10f // its mostly just silly
    override val name: String = "Axial Overclocking"
    override val spriteId: String = "graphics/icons/industry/mining.png"

    override fun applyInCombat(station: ShipAPI) {
        station.mutableStats.maxTurnRate.modifyMult(id, TURN_MULT)
        station.mutableStats.turnAcceleration.modifyMult(id, TURN_MULT)

        val industryId = (getStationIndustry()?.spec?.id) ?: return
        if (stationTypesToGetExtraWeaponTurnrate.contains(industryId)) {
            for (module in station.childModulesCopy + station) {
                module.mutableStats.weaponTurnRateBonus.modifyMult(id, WEAPON_TURNRATE_MULT)
            }
        }
    }
}