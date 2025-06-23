package niko_SA.augments.threat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.CombatEngineAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.backend.ui.components.LunaUITextFieldWithSlider
import niko_SA.augments.core.AugmentMenuDialogueDelegate
import org.lazywizard.lazylib.MathUtils

class KineticFragments: ThreatAugment() {

    companion object {
        const val ACCEPTABLE_RANGE = 3000f * 2f
        const val REFIRE_VAR_MIN = 0.5f
        const val REFIRE_VAR_MAX = 1.5f
    }

    enum class Level(val apMult: Float, val extraDelayBetweenStrikes: Float, val frontend: Int) {
        ONE(1f, 1.5f, 1),
        TWO(2f, 0.8f, 2),
        THREE(3f, 0.5f, 3);
    }

    var level: Level = Level.ONE

    var useAPMult = true
    override fun getAugmentCost(): Float {
        if (useAPMult) return super.getAugmentCost() * level.apMult
        return super.getAugmentCost()
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Coordinates nearby fragments into highly polarized swarms capable of destroying hostile shield grids.",
            0f
        )
        tooltip.addPara(
            "Effectively mimics the %s weapon, but on a much larger scale and with greater range.",
            5f,
            Misc.getHighlightColor(),
            "kinetic fragments"
        )
        tooltip.addPara(
            "Uses %s fragments per volley.",
            5f,
            Misc.getHighlightColor(),
            "five"
        )

        tooltip.addPara(
            "This augment is %s. It has %s modes that can be changed by a slider while applied.",
            5f,
            Misc.getHighlightColor(),
            "modular", "three"
        )

        tooltip.setBgAlpha(0.9f)

        val table = tooltip.beginTable(
            Misc.getBasePlayerColor(), Misc.getDarkPlayerColor(), Misc.getBrightPlayerColor(),
            20f, true, true,
            *arrayOf<Any>("Mode", 70f, "Augment Cost", 130f, "Delay between volleys", 160f)
        )
        val hl = Misc.getHighlightColor()
        useAPMult = false
        tooltip.addRow(
            Alignment.MID, hl, "One",
            Alignment.MID, hl, (getAugmentCost() * Level.ONE.apMult).toString(),
            Alignment.MID, hl, (Level.ONE.extraDelayBetweenStrikes).toString()
        )
        tooltip.addRow(
            Alignment.MID, hl, "Two",
            Alignment.MID, hl, (getAugmentCost() * Level.TWO.apMult).toString(),
            Alignment.MID, hl, (Level.TWO.extraDelayBetweenStrikes).toString()
        )
        tooltip.addRow(Alignment.MID, hl, "Three",
            Alignment.MID, hl, (getAugmentCost() * Level.THREE.apMult).toString(),
            Alignment.MID, hl, (Level.THREE.extraDelayBetweenStrikes).toString()
        )

        tooltip.addTable("", 0, 15f)
        useAPMult = true
    }

    override fun modifyAugmentMenu(tooltip: TooltipMakerAPI, panel: CustomPanelAPI?, buttonPanel: CustomPanelAPI?, delegate: AugmentMenuDialogueDelegate) {
        if (panel == null) return
        val slider = LunaUITextFieldWithSlider<Int>(level.frontend, Level.ONE.frontend.toFloat(), 3f, 100f, 50f, "SA_kineticFragSlider", "SA_kineticFragSlider", panel, tooltip)
        slider.onUpdate { events ->
            if (!events.isEmpty()) {
                if (slider.value != level.frontend) {
                    val foundLevel = Level.entries.first { it.frontend == slider.value }

                    useAPMult = false
                    if (getAPChangeInapplicableReason(getAugmentCost() * foundLevel.apMult) == null) {
                        level = foundLevel
                    } else {
                        slider.value = level.frontend
                    }
                    useAPMult = true
                    delegate.callback?.let { callback -> delegate.regenerateDialog(callback) }
                }
            }
        }
        slider.position?.inRMid(5f)
    }

    override fun applyInCombat(station: ShipAPI) {
        val plugin = KineticFragmentsMimic(station)
        plugin.acceptableRange = ACCEPTABLE_RANGE
        plugin.modifyHostShip = { it.mutableStats.missileWeaponRangeBonus.modifyMult("SA_kinFragRange", 2.5f) }
        plugin.unModifyHostShip = { it.mutableStats.missileWeaponRangeBonus.unmodify("SA_kinFragRange") }

        plugin.fireDelay = level.extraDelayBetweenStrikes

        plugin.refireVarMin = REFIRE_VAR_MIN
        plugin.refireVarMax = REFIRE_VAR_MAX

        plugin.autoFire = true

        Global.getCombatEngine().addPlugin(plugin)
    }

    class KineticFragmentsMimic(station: ShipAPI): FragWeaponMimic("kinetic_fragments", "kinetic_fragments_fire", station, 5f) {
        override fun getWeightForShip(ship: ShipAPI): Float {
            val base = super.getWeightForShip(ship)
            var total = base

            if (ship.shield == null) {
                total *= 0.1f
            } else if (ship.shield.isOff) {
                total *= 0.25f
            }

            return total
        }
    }
}