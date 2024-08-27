package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import niko_SA.genericIndustries.stationAttachment

class fluxShunt: stationAttachment() {
    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.hardFluxDissipationFraction.modifyM
        }
    }

    override fun apply() {
        return
    }
}