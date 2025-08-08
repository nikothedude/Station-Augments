package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.ShipCommand
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.ReflectionUtils
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.augments.core.stationAttachment

class regenerativeDrones() : stationAttachment() {

    companion object {
        const val AMMO_PER_SECOND_INCREMENT = 0.05f // 20 secs
    }
    override val stationTypeWhitelist: HashSet<String> = HashSet()

    override fun applyInCombat(station: ShipAPI) {
        val system = station.system ?: return
        if (ReflectionUtils.hasMethodOfName("setDeploy", system)) { // correct system type
            ReflectionUtils.invoke("setDeploy", system)
            val ammoTracker = ReflectionUtils.invoke("getAmmoTracker", system)!!
            val perSec = station.system.ammoPerSecond
            ReflectionUtils.invoke("setAmmoPerSecond", ammoTracker, perSec + AMMO_PER_SECOND_INCREMENT)
            Global.getCombatEngine().addPlugin(PreventRecallScript(station))
        }
    }

    override fun getUnavailableReason(): String? {
        val superResult = super.getUnavailableReason()
        if (superResult != null) return superResult

        if (!stationHasDrones()) return "No drone system"

        return null
    }

    // otherwise the station recalls them constantly for some reason
    class PreventRecallScript(val station: ShipAPI?) : BaseEveryFrameCombatPlugin() {
        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)
            station?.blockCommandForOneFrame(ShipCommand.USE_SYSTEM)
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Special fabricators can be installed into the drone bays of the station core, allowing on-the-fly construction " +
                "of combat drones.",
            5f
        )

        tooltip.addPara(
            "If a drone is lost, it will be replaced %s seconds later.",
            5f,
            Misc.getHighlightColor(),
            "${(1 / AMMO_PER_SECOND_INCREMENT).trimHangingZero()}"
        )
    }

    override fun getBlueprintValue(): Int {
        return 7000
    }
}