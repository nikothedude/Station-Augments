package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import niko_SA.genericIndustries.stationAttachment

class axialOverclocking: stationAttachment() {
    companion object {
        const val TURN_MULT = 6f

        /** Otherwise, these stations cant actually turn their guns fast enough to aim at anything. */
        val stationTypesToGetExtraWeaponTurnrate: HashSet<String> = hashSetOf(Industries.ORBITALSTATION_MID, Industries.BATTLESTATION_MID, Industries.STARFORTRESS_MID)
        const val WEAPON_TURNRATE_MULT = 5f
    }

    override val augmentCost: Float = 10f // its mostly just silly

    override fun applyInCombat(station: ShipAPI) {
        station.system.ammo = 500

        station.mutableStats.maxTurnRate.modifyMult(id, TURN_MULT)
        station.mutableStats.turnAcceleration.modifyMult(id, TURN_MULT)

        val industryId = (getStationIndustry()?.spec?.id) ?: return
        if (stationTypesToGetExtraWeaponTurnrate.contains(industryId)) {
            for (module in station.childModulesCopy + station) {
                module.mutableStats.weaponTurnRateBonus.modifyMult(id, WEAPON_TURNRATE_MULT)
            }
        }
    }

    override fun apply() {
        return
    }
}