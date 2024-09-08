package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class armoredWeaponMounts(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    companion object {
        const val RECOIL_BONUS = 25f
        const val HEALTH_BONUS = 100f
        const val ARMOR_BONUS = 10f
        const val TURN_PENALTY = 25f
    }

    override val augmentCost: Float = 9f
    override val name: String = "Armored Weapon Mounts"
    override val spriteId: String = "graphics/hullmods/armored_weapon_emplacements.png"

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            val stats = module.mutableStats

            stats.armorBonus.modifyPercent(id, ARMOR_BONUS)
            stats.weaponHealthBonus.modifyPercent(id, HEALTH_BONUS)
            stats.weaponTurnRateBonus.modifyMult(id, 1f - TURN_PENALTY * 0.01f)

            stats.maxRecoilMult.modifyMult(id, 1f - 0.01f * RECOIL_BONUS)
            stats.recoilPerShotMult.modifyMult(id, 1f - 0.01f * RECOIL_BONUS)
            // slower recoil recovery, also, to match the reduced recoil-per-shot
            // overall effect is same as without skill but halved in every respect
            stats.recoilDecayMult.modifyMult(id, 1f - 0.01f * RECOIL_BONUS)
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

        val para = tooltip.addPara(
            "Increases the durability of all weapons by %s and reduces recoil by %s, but decreases weapon turn rate by %s." +
            " Also increases the station's armor by %s.",
            5f,
            Misc.getHighlightColor(),
            "${HEALTH_BONUS.toInt()}%", "${RECOIL_BONUS.toInt()}%", "${TURN_PENALTY.toInt()}%", "${ARMOR_BONUS.toInt()}%"
        )
        para.setHighlightColors(Misc.getHighlightColor(), Misc.getHighlightColor(), Misc.getNegativeHighlightColor(), Misc.getHighlightColor())
    }
}