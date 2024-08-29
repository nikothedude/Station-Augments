package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.combat.entities.Ship
import com.fs.starfarer.combat.systems.Oo0O
import niko_SA.genericIndustries.stationAttachment

class regenerativeDrones: stationAttachment() {

    companion object {
        const val AMMO_PER_SECOND_INCREMENT = 0.05f // 20 secs
    }

    override val augmentCost: Float = 15f
    // i cant be sure other star forts will have drones
    override val stationTypeWhitelist: HashSet<String> = hashSetOf(
        Industries.STARFORTRESS,
        Industries.STARFORTRESS_MID,
        Industries.STARFORTRESS_HIGH
    )

    override fun applyInCombat(station: ShipAPI) {
        // first cast - to drone ship system, found in com.fs.starfarer.combat.systems
        // second cast - to some... thing. i dont really know, you find it by tracking getAmmoPerSecond() down the inheritance chain
        val castedSystem = station.system as? Oo0O ?: return
        castedSystem.chargeTracker.Ô00000().ammoPerSecond += AMMO_PER_SECOND_INCREMENT
        Global.getCombatEngine().addPlugin(PreventRecallScript(station))
    }

    class PreventRecallScript(val station: ShipAPI?) : BaseEveryFrameCombatPlugin() {
        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)
            station?.blockCommandForOneFrame(ShipCommand.USE_SYSTEM)
        }
    }

    override fun apply() {
        return
    }

    override fun getNeededStationTypeText(): String {
        return "a vanilla star fortress"
    }
}