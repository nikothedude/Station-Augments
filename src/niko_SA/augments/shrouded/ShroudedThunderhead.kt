package niko_SA.augments.shrouded

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import niko_SA.augments.shrouded.shroudedMantle.ShroudedMantleCombatPlugin

class ShroudedThunderhead: ShroudedAugment() {

    override fun applyInCombat(station: ShipAPI) {
        super.applyInCombat(station)

        Global.getCombatEngine().addPlugin(ShroudedThunderheadCombatPlugin(station))

        return
    }

    class ShroudedThunderheadCombatPlugin(
        val station: ShipAPI
    ): BaseEveryFrameCombatPlugin()
}