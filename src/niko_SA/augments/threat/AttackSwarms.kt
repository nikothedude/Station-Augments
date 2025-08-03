package niko_SA.augments.threat

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.FighterWingAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.combat.threat.AttackSwarmPhaseModeScript
import com.fs.starfarer.api.impl.combat.threat.RoilingSwarmEffect
import com.fs.starfarer.api.impl.combat.threat.SwarmLauncherEffect
import com.fs.starfarer.api.impl.combat.threat.ThreatSwarmAI
import com.fs.starfarer.api.impl.combat.threat.VoltaicDischargeOnFireEffect
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.Alignment
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import lunalib.backend.ui.components.LunaUITextFieldWithSlider
import niko_SA.augments.core.AugmentMenuDialogueDelegate

class AttackSwarms: ThreatAugment() {

    companion object {
        const val ACCEPTABLE_RANGE = Float.MAX_VALUE
        const val REFIRE_VAR_MIN = 0.5f
        const val REFIRE_VAR_MAX = 1.5f
    }

    enum class Level(val apMult: Float, val maxConcurrentSwarms: Int, val frontend: Int, val volatic: Boolean = false) {
        ONE(1f, 2, 1),
        TWO(2f, 4, 2),
        THREE(3f, 4, 3, true);
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
            "Coordinates nearby fragments into highly organized attack formations that attack hostile ships, regardless of distance.",
            0f
        )
        tooltip.addPara(
            "Effectively mimics the %s weapon, but on a larger scale.",
            5f,
            Misc.getHighlightColor(),
            "swarm launcher"
        )
        tooltip.addPara(
            "Uses %s fragments per volley.",
            5f,
            Misc.getHighlightColor(),
            "fifty"
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
            *arrayOf<Any>("Mode", 70f, "Augment Cost", 130f, "Swarms", 80f, "Voltaic", 100f)
        )
        val hl = Misc.getHighlightColor()
        useAPMult = false
        tooltip.addRow(
            Alignment.MID, hl, "One",
            Alignment.MID, hl, (getAugmentCost() * Level.ONE.apMult).toString(),
            Alignment.MID, hl, (Level.ONE.maxConcurrentSwarms).toString(),
            Alignment.MID, Misc.getNegativeHighlightColor(), "No"
        )
        tooltip.addRow(
            Alignment.MID, hl, "Two",
            Alignment.MID, hl, (getAugmentCost() * Level.TWO.apMult).toString(),
            Alignment.MID, hl, (Level.TWO.maxConcurrentSwarms).toString(),
            Alignment.MID, Misc.getNegativeHighlightColor(), "No"
        )
        tooltip.addRow(
            Alignment.MID, hl, "Three",
            Alignment.MID, hl, (getAugmentCost() * Level.THREE.apMult).toString(),
            Alignment.MID, hl, (Level.THREE.maxConcurrentSwarms).toString(),
            Alignment.MID, Misc.getPositiveHighlightColor(), "Yes"
        )

        tooltip.addTable("", 0, 15f)
        useAPMult = true
    }

    override fun modifyAugmentMenu(
        tooltip: TooltipMakerAPI,
        panel: CustomPanelAPI?,
        buttonPanel: CustomPanelAPI?,
        delegate: AugmentMenuDialogueDelegate
    ) {
        if (panel == null) return
        val slider = LunaUITextFieldWithSlider<Int>(
            level.frontend,
            Level.ONE.frontend.toFloat(),
            3f,
            100f,
            50f,
            "SA_launcherFragSlider",
            "SA_launcherFragSlider",
            panel,
            tooltip
        )
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
        val plugin = AttackSwarmMimic(station, level.volatic)
        plugin.acceptableRange = ACCEPTABLE_RANGE

        plugin.bonusCanFireCheck = end@{ ship, swarm ->
            val wing = plugin.fakeWeapon.custom as? FighterWingAPI
            if (wing != null && wing.wingMembers.size >= level.maxConcurrentSwarms) {
                return@end false
            }
            return@end true
        }

        //plugin.refireVarMin = REFIRE_VAR_MIN
        //plugin.refireVarMax = REFIRE_VAR_MAX

        plugin.autoFire = true

        Global.getCombatEngine().addPlugin(plugin)
    }

    class AttackSwarmMimic(station: ShipAPI, val voltaic: Boolean) : FragWeaponMimic("swarm_launcher", "threat_swarm_launched", station, 50f, Float.MAX_VALUE, false, false) {

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            super.advance(amount, events)

            val engine = Global.getCombatEngine()
            if (engine.isPaused) return

            val plugin = fakeWeapon.effectPlugin
            plugin.advance(amount, engine, fakeWeapon)
            if (!voltaic) return
            val wing = fakeWeapon.custom as? FighterWingAPI ?: return
            for (member in wing.wingMembers) {
                val swarm = RoilingSwarmEffect.getSwarmFor(member) ?: continue
                if (!ThreatSwarmAI.isAttackSwarm(member)) continue
                if (VoltaicDischargeOnFireEffect.SWARM_TAG_PHASE_MODE in swarm.params.tags) continue
                AttackSwarmPhaseModeScript(member, Float.MAX_VALUE)
                member.mutableStats.hullBonus.modifyMult("SA_volaticAttackSwarmsHull", 2.5f)
            }
        }
    }
}