package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import org.lwjgl.util.vector.Vector2f

class backlineStation: stationAttachment() {

    override val incompatibleAugments: MutableSet<String> = mutableSetOf("SA_movableStation")

    override fun applyInCombat(station: ShipAPI) {
        Global.getCombatEngine().addPlugin(
            BacklineStationScript(station)
        )
    }

    class BacklineStationScript(val station: ShipAPI): BaseEveryFrameCombatPlugin() {
        var timesRan = 0f
        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            timesRan++
            if (timesRan >= 2) {
                Global.getCombatEngine().removePlugin(this)
                val oldFixedLoc = Vector2f(station.fixedLocation)
                val engine = Global.getCombatEngine()
                val height = engine.mapHeight
                val player = station.owner == 0
                val sign = if (player) -1f else 1f
                val newLoc = Vector2f(oldFixedLoc)
                newLoc.set(0f, height * 0.28f * sign)

                station.fixedLocation = newLoc
                station.location.set(newLoc.x, newLoc.y)

                for (module in station.childModulesCopy + station) {
                    for (wing in module.allWings) {
                        for (fighter in wing.wingMembers) {
                            fighter.location.set(station.location.x, station.location.y)
                        }
                    }
                }
                if (station.deployedDrones != null) {
                    station.deployedDrones.forEach {
                        it.location.set(station.location.x, station.location.y)
                    }
                }
            }
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Deploys the station far on the defender's end of the battlefield, allowing for a much easier defense.",
            5f
        )

        tooltip.addPara(
            "Allows the station to guard the defender spawnpoint, as well as quickly receive reinforcements.",
            5f
        ).color = Misc.getGrayColor()
    }

}