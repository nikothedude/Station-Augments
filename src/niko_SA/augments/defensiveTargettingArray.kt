// doesnt work, fighters ignore the tag

package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class defensiveTargettingArray: stationAttachment() {

    companion object {
        const val PD_DAMAGE_BONUS = 50f
        const val RANGE_BONUS = 200f
    }

    override fun applyInCombat(station: ShipAPI) {
        val engine = Global.getCombatEngine()

        for (module in station.childModulesCopy + station) {
            engine.addPlugin(DefensiveTargetingArrayScript(module, id))
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Standard fighter communication links are re-purposed to provide short-range targeting data. This targeting data is enhanced on stations, " +
                "which often house enough equipment to make fighters' range similar to that of it's main batteries.",
            5f
        )

        tooltip.addPara(
            "Increases fighter damage to missiles and other fighters by %s, but makes the fighters unable to leave the vicinity of their ship. Additionally, fighter weapon range is increased by a sizable %s.",
            5f,
            Misc.getHighlightColor(),
            "${PD_DAMAGE_BONUS.toInt()}%", "${RANGE_BONUS.toInt()}%"
        )
    }

    override fun getBlueprintValue(): Int {
        return 25000
    }

    class DefensiveTargetingArrayScript(val module: ShipAPI, val id: String): BaseEveryFrameCombatPlugin() {
        val interval = IntervalUtil(0.05f, 0.06f)

        override fun advance(amount: Float, events: MutableList<InputEventAPI>?) {
            super.advance(amount, events)

            interval.advance(amount)
            if (interval.intervalElapsed()) {
                alterFighters()
            }
        }

        private fun alterFighters() {
            for (wing in module.allWings) {
                for (member in wing.wingMembers) {
                    val stats = member.mutableStats
                    stats.damageToFighters.modifyFlat(id, PD_DAMAGE_BONUS / 100f)
                    stats.damageToMissiles.modifyFlat(id, PD_DAMAGE_BONUS / 100f)

                    stats.ballisticWeaponRangeBonus.modifyFlat(id, RANGE_BONUS)
                    stats.energyWeaponRangeBonus.modifyFlat(id, RANGE_BONUS)

                    if (wing.spec.isRegularFighter || wing.spec.isBomber || wing.spec.isAssault || wing.spec.isInterceptor) {
                        member.addTag(Tags.WING_STAY_IN_FRONT_OF_SHIP)
                    }
                }
            }
        }
    }
}