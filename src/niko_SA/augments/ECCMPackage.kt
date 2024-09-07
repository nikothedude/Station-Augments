package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class ECCMPackage(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    companion object {

        var MISSILE_SPEED_BONUS = 25f
        var MISSILE_RANGE_MULT = 0.8f
        var MISSILE_ACCEL_BONUS = 150f
        var MISSILE_RATE_BONUS = 50f
        var MISSILE_TURN_ACCEL_BONUS = 150f

        var EW_PENALTY_MULT = 0.5f

        var ECCM_CHANCE = 0.5f
    }

    override val augmentCost: Float = 15f
    override val name: String = "ECCM Package"
    override val spriteId: String = "graphics/hullmods/eccm_package.png"

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            val stats = module.mutableStats

            stats.missileMaxSpeedBonus.modifyPercent(id, MISSILE_SPEED_BONUS)
            stats.missileWeaponRangeBonus.modifyMult(id, MISSILE_RANGE_MULT)
            stats.missileAccelerationBonus.modifyPercent(id, MISSILE_ACCEL_BONUS)
            stats.missileMaxTurnRateBonus.modifyPercent(id, MISSILE_RATE_BONUS)
            stats.missileTurnAccelerationBonus.modifyPercent(id, MISSILE_TURN_ACCEL_BONUS)
        }
        station.mutableStats.dynamic.getMod(Stats.ELECTRONIC_WARFARE_PENALTY_MOD).modifyMult(id, EW_PENALTY_MULT)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        tooltip.addPara(
            "Reduces the chance for missiles launched by station modules to be affected by electronic counter-measures and flares by %s.\n" +
            "\n" +
            "A CPU core adjunct in each missile increases missile top speed by %s and missile maneuverability by %s, as well as significantly improving the guidance algorithm.\n" +
            "\n" +
            "Also reduces the weapon range reduction due to superior enemy Electronic Warfare by %s.",
            5f,
            Misc.getHighlightColor(),
            "${ECCM_CHANCE * 100f}%", "$MISSILE_SPEED_BONUS%", "$MISSILE_RATE_BONUS%", "${(1f - EW_PENALTY_MULT) * 100f}%"
        )
    }
}