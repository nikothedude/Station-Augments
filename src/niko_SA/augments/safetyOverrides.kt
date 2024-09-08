package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent
import java.awt.Color
import java.util.*
import kotlin.math.absoluteValue


class safetyOverrides(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    override val name: String = "Safety Shunt"
    override val spriteId: String = "graphics/hullmods/safety_overrides.png"

    companion object {
        const val ROF_MULT = 1.7f

        val WEAPON_COLOR = Color(255,100,255,255)
        const val TURN_RATE_MULT = 1.5f

        const val RECOIL_MULT = 2f

        const val FLUX_DISSIPATION_MULT = 3f
    }

    override val augmentCost: Float = 19f

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.recoilPerShotMult.modifyMult(id, RECOIL_MULT)
            module.mutableStats.maxRecoilMult.modifyMult(id, RECOIL_MULT)

            module.mutableStats.turnAcceleration.modifyMult(id, TURN_RATE_MULT)
            module.mutableStats.maxTurnRate.modifyMult(id, TURN_RATE_MULT)

            module.mutableStats.peakCRDuration.modifyMult(id, 0.000017f)
            module.mutableStats.crLossPerSecondPercent.modifyFlat(id, 0.5f)

            module.mutableStats.ballisticRoFMult.modifyMult(id, ROF_MULT)
            module.mutableStats.energyRoFMult.modifyMult(id, ROF_MULT)

            module.mutableStats.fluxDissipation.modifyMult(id, FLUX_DISSIPATION_MULT)
            module.mutableStats.ventRateMult.modifyMult(id, 0f)

            module.setWeaponGlow(0.7f, WEAPON_COLOR,
                EnumSet.of(WeaponAPI.WeaponType.BALLISTIC, WeaponAPI.WeaponType.ENERGY))
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        val testTooltip = tooltip.addPara(
            "Though typically considered a \"bad idea\", stations do indeed have safeties, and they can in fact be disabled. Doing so " +
            "increases non-missile weapon firerate by %s, though the unmonitored increase typically decreases accuracy by %s for those same weapons." +
            "\n" +
            "The flux dissipation rate, including that of additional vents, is increased by a factor of %s." +
            "\n" +
            "The immense strain on the flux conduits %s, and starts the station with %s." +
            "\n" +
            "Also increases station turnrate by %s.",
            5f,
            Misc.getHighlightColor(),
            toPercent((1 - ROF_MULT).absoluteValue),
            toPercent((1 - RECOIL_MULT).absoluteValue),
            toPercent(FLUX_DISSIPATION_MULT),

            "allows the station to lose CR",
            "low PPT",

            toPercent((1 - TURN_RATE_MULT).absoluteValue)
        )
        testTooltip.setHighlightColors(
            Misc.getHighlightColor(),
            Misc.getNegativeHighlightColor(),
            Misc.getHighlightColor(),
            Misc.getNegativeHighlightColor(),
            Misc.getNegativeHighlightColor()
        )
    }

    override fun getBlueprintValue(): Int {
        return 1000
    }
}