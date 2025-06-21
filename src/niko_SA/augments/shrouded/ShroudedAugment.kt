package niko_SA.augments.shrouded

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.dweller.HumanShipShroudCreator
import com.fs.starfarer.api.ui.TooltipMakerAPI
import niko_SA.augments.core.stationAttachment

abstract class ShroudedAugment: stationAttachment() {

    val creator = StationShroudCreator(this)

    override fun applyInCombat(station: ShipAPI) {
        applyGenericShroudedEffects(station)
    }

    private fun applyGenericShroudedEffects(station: ShipAPI) {
        creator.masterStation = station
        creator.initInCombat(station)
        for (module in station.childModulesCopy) {
            creator.initInCombat(module)
        }
        creator.masterStation = null
    }

    override fun getBlueprintValue(): Int {
        return 350000
    }
}