package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_mathUtils.trimHangingZero
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent

class efficiencyOverhaul: stationAttachment() {

    companion object {
        const val UPKEEP_MULT = 0.7f
        const val CR_RECOVERY_PERCENT = 50f
    }

    override fun apply() {
        super.apply()

        val industry = getStationIndustry() ?: return
        industry.upkeep.modifyMult(id, UPKEEP_MULT, getName())

        val fleet = getStationFleet() ?: return

        fleet.fleetData.membersListCopy.forEach {
            it.stats.baseCRRecoveryRatePercentPerDay.modifyPercent(id, CR_RECOVERY_PERCENT, getName())
            it.stats.repairRatePercentPerDay.modifyPercent(id, CR_RECOVERY_PERCENT, getName())
        }
    }

    override fun unapply() {
        super.unapply()

        val industry = getStationIndustry() ?: return
        industry.upkeep.unmodify(id)

        val fleet = getStationFleet() ?: return

        fleet.fleetData.membersListCopy.forEach {
            it.stats.baseCRRecoveryRatePercentPerDay.unmodify(id)
            it.stats.repairRatePercentPerDay.unmodify(id)
        }
    }

    override fun applyInCombat(station: ShipAPI) {
        return
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
       super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Reduces upkeep cost by %s.\n" +
            "\n" +
            "Increases the combat readiness recovery and repair rates by %s.",
            5f,
            Misc.getHighlightColor(),
            toPercent(1 - UPKEEP_MULT),
            "${CR_RECOVERY_PERCENT.trimHangingZero()}%"
        )

        tooltip.addPara(
            "This augment remains active even if the station is disrupted.",
            5f
        ).color = Misc.getGrayColor()
        tooltip.addPara(
            "This augment does not increase the rate of repair for a fully disrupted station.",
            0f
        ).color = Misc.getGrayColor()
    }
}