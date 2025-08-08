package niko_SA.augments

import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.combat.WeaponAPI
import com.fs.starfarer.api.combat.WeaponAPI.AIHints
import com.fs.starfarer.api.combat.WeaponAPI.WeaponSize
import com.fs.starfarer.api.combat.WeaponAPI.WeaponType
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import data.utilities.niko_MPC_battleUtils.isPD
import niko_SA.augments.core.stationAttachment

class IDPAI: stationAttachment() {

    companion object {
        const val DAMAGE_BONUS = 50f
    }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            val stats = module.mutableStats

            //boolean sMod = isSMod(stats);

            //stats.getRecoilPerShotMultSmallWeaponsOnly().modifyMult(id, 0f);
            //stats.getRecoilDecayMult().modifyMult(id, 10f);
            stats.dynamic.getMod(Stats.PD_IGNORES_FLARES).modifyFlat(id, 1f)
            stats.dynamic.getMod(Stats.PD_BEST_TARGET_LEADING).modifyFlat(id, 1f)
            stats.damageToMissiles.modifyPercent(id, DAMAGE_BONUS)

            for (wpn in module.allWeapons) {
                if (wpn.hasAIHint(WeaponAPI.AIHints.PD)) continue
                if (wpn.type == WeaponType.MISSILE || wpn.hasAIHint(AIHints.STRIKE)) continue

                wpn.setPDAlso(true)
            }
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Gives all point-defense weapons the ability to identify - and ignore - decoy flares. All point-defense weapons get the best possible target leading, regardless of combat readiness, and all damage to missiles is increased by %s." +
            "\n\nIn addition, all weapons gain the ability to fire on missiles if no other target is available.",
            5f,
            Misc.getPositiveHighlightColor(),
            "${DAMAGE_BONUS.toInt()}%"
        )
    }
}