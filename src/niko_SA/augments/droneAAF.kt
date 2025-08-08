// drones dont use the AAF theyre given
package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import niko_SA.subsystems.WeaponryOverdriveSubsystem
import org.magiclib.subsystems.MagicSubsystemsManager.addSubsystemToShip

class droneAAF: stationAttachment() {

    override fun applyInCombat(station: ShipAPI) {
        val engine = Global.getCombatEngine()
        engine.addPlugin(DroneAAFScript(station, id))
    }

    override fun getUnavailableReason(): String? {
        val superResult = super.getUnavailableReason()
        if (superResult != null) return superResult

        if (!stationHasDrones()) return "No drone system"

        return null
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Refits station-launched drones with a subsystem that %s for a brief duration.",
            5f,
            Misc.getHighlightColor(),
            "doubles ROF of non-missile weapons"
        )
    }

    class DroneAAFScript(val station: ShipAPI, val id: String): BaseEveryFrameCombatPlugin() {
        val checkedDrones = HashSet<ShipAPI>()

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            /*if (Global.getCombatEngine().playerShip?.isShuttlePod == false) {
                addSubsystemToShip(Global.getCombatEngine().playerShip, AAFSubsystem(Global.getCombatEngine().playerShip))
                Global.getCombatEngine().removePlugin(this)
            }*/
            if (station.deployedDrones == null) return
            for (drone in station.deployedDrones) {
                if (checkedDrones.contains(drone)) continue

                addSubsystemToShip(drone, WeaponryOverdriveSubsystem(drone))

                checkedDrones += drone
            }
        }
    }
}