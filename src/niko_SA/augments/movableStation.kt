package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class movableStation: stationAttachment() {

    companion object {
        const val TARGET_SPEED = 40f
        /// Acceleration, deceleration...
        const val TARGET_MANUVERABILITY = 15f
    }

    override val incompatibleAugments: MutableSet<String> = mutableSetOf("SA_backlineStation")

    override fun applyInCombat(station: ShipAPI) {
        station.fixedLocation = null // yeah. YEAH. WE OUT HERE.

        val startingMax = station.maxSpeed
        val speedAdjustment = (TARGET_SPEED - startingMax)
        station.mutableStats.maxSpeed.modifyFlat(id, speedAdjustment)

        val startingAccel = station.acceleration
        val startingDecel = station.deceleration

        val accelAdjustment = (TARGET_MANUVERABILITY - startingAccel)
        val decelAdjustment = (TARGET_MANUVERABILITY - startingDecel)

        station.mutableStats.acceleration.modifyFlat(id, accelAdjustment)
        station.mutableStats.deceleration.modifyFlat(id, decelAdjustment)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Attaches macro-thrusters to the hull of the station, allowing for limited movement in-combat.",
            5f
        )

        tooltip.addPara(
            "The station moves at a rate of %s, with acceleration/deceleration of %s.",
            5f,
            Misc.getHighlightColor(),
            "${TARGET_SPEED.toInt()}su", "${TARGET_MANUVERABILITY.toInt()}su/s"
        )
    }
}