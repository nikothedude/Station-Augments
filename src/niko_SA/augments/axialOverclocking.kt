package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.CircularOrbitWithSpin
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent

class axialOverclocking : stationAttachment() {
    companion object {
        const val TURN_MULT = 6f

        /** Otherwise, these stations cant actually turn their guns fast enough to aim at anything. */
        val stationTypesToGetExtraWeaponTurnrate: HashSet<String> = hashSetOf(Industries.ORBITALSTATION_MID, Industries.BATTLESTATION_MID, Industries.STARFORTRESS_MID)
        const val WEAPON_TURNRATE_MULT = 5f
    }

    override fun apply() {
        super.apply()

        val orbit = (getStationCampaignEntity()?.orbit as? CircularOrbitWithSpin) ?: return

        orbit.spinVel *= TURN_MULT
    }

    override fun unapply() {
        super.unapply()

        val orbit = (getStationCampaignEntity()?.orbit as? CircularOrbitWithSpin) ?: return

        orbit.spinVel /= TURN_MULT
    }

    override fun applyInCombat(station: ShipAPI) {
        station.mutableStats.maxTurnRate.modifyMult(id, TURN_MULT)
        station.mutableStats.turnAcceleration.modifyMult(id, TURN_MULT)

        val industryId = (getStationIndustry()?.spec?.id) ?: return
        if (stationTypesToGetExtraWeaponTurnrate.contains(industryId)) {
            for (module in station.childModulesCopy + station) {
                module.mutableStats.weaponTurnRateBonus.modifyMult(id, WEAPON_TURNRATE_MULT)
            }
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "The axial actuators on most stations are actually often operated well below maximum thresholds, making it trivial to increase the spin rate.",
            5f
        )

        val para = tooltip.addPara(
            "Increases station turn-rate by %s.",
            5f,
            Misc.getHighlightColor(),
            toPercent(TURN_MULT - 1)
        )
    }
}