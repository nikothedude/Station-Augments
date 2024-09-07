package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.systems.Oo0O
import niko_SA.augments.core.stationAttachment

class regenerativeDrones(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    override val manufacturer: String = "Ko Combine"
    override val name: String = "Reserve drone bay"
    override val spriteId: String = "graphics/icons/industry/mining.png"

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
        castedSystem.setDeploy()
        castedSystem.chargeTracker.Ô00000().ammoPerSecond += AMMO_PER_SECOND_INCREMENT
        Global.getCombatEngine().addPlugin(PreventRecallScript(station))
    }

    // otherwise the station recalls them constantly for some reason
    class PreventRecallScript(val station: ShipAPI?) : BaseEveryFrameCombatPlugin() {
        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)
            station?.blockCommandForOneFrame(ShipCommand.USE_SYSTEM)
        }
    }

    override fun getNeededStationTypeText(): String {
        return "a vanilla star fortress"
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "Special fabricators can be installed into the drone bays of the station core, allowing on-the-fly construction " +
                "of combat drones.",
            5f
        )

        tooltip.addPara(
            "If a drone is lost, it will be replaced %s seconds later.",
            5f,
            Misc.getHighlightColor(),
            "${1 / AMMO_PER_SECOND_INCREMENT}"
        )
    }

    override fun getBlueprintValue(): Int {
        return 7000
    }
}