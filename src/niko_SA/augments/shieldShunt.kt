package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShieldAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.ai.BasicShipAI
import com.fs.starfarer.combat.ai.attack.AttackAIModule
import com.fs.starfarer.combat.ai.ooOO
import com.fs.starfarer.combat.entities.Ship
import niko_SA.ReflectionUtils.get
import niko_SA.ReflectionUtils.set
import niko_SA.SA_debugUtils
import niko_SA.SA_settings
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent
import org.lwjgl.util.vector.Vector2f
import kotlin.math.absoluteValue

/** TODO: In the case of updating, check comments below to see what to change*/
class shieldShunt() : stationAttachment() {

    companion object {
        const val ARMOR_MULT = 1.4f
        const val HULL_MULT = 1.1f
    }

    override fun applyInCombat(station: ShipAPI) {
        val shipAPIship = station as ShipAPI
        for (module in station.childModulesCopy + station) {
            val moduleShipAPI = module as ShipAPI
            if (!module.isHullDamageable()) continue
            if (module.shield != null || module.hullSpec.hasTag(Tags.MODULE_HULL_BAR_ONLY)) { // armor
                module.setShield(ShieldAPI.ShieldType.PHASE, 0f, 1f, 0f)
                if (module.mutableStats.fluxCapacity.modifiedInt < 5) {
                    module.mutableStats.fluxCapacity.modifyFlat(id, 5f)
                }
                if (module.mutableStats.fluxDissipation.modifiedInt < 5) {
                    module.mutableStats.fluxDissipation.modifyFlat(id, 5f)
                }

                val shipAI = module.ai
                if (SA_settings.isWindows && shipAI is BasicShipAI) { // no compatability for custom ais, sorry
                    try {
                        val damperSpec = Global.getSettings().getShipSystemSpec("SA_KKdamper") as com.fs.starfarer.loading.specs.`do`
                        set(
                            "phaseCloak",
                            module,
                            damperSpec.createSystem(module as Ship?)
                        )

                        val threatEvalAI = get("threatEvalAI", moduleShipAPI.ai, BasicShipAI::class.java)
                        val attackAI = get("attackAI", moduleShipAPI.ai, BasicShipAI::class.java)
                        val flockingAI = get("flockingAI", moduleShipAPI.ai, BasicShipAI::class.java)

                        val newSystemAI = damperSpec.createSystemAI(
                            module, moduleShipAPI.aiFlags,
                            threatEvalAI as? com.fs.starfarer.combat.ai.D,
                            attackAI as? AttackAIModule,
                            flockingAI as? com.fs.starfarer.combat.ai.movement.A,
                            shipAI as? (com.fs.starfarer.combat.ai.movement.maneuvers.M.o) //ShipAI obf class
                        )
                        // v mimics a anonymous wrapper the convinces the game to laod a systemai as a shieldai. see basicshipai for more, its in its constructor
                        val testValTwo = object : com.fs.starfarer.combat.ai.F {
                            override fun o00000(
                                p0: Float,
                                p1: com.fs.starfarer.combat.ai.D?,
                                p2: Vector2f?,
                                p3: Vector2f?,
                                p4: Ship?
                            ) {
                                moduleShipAPI
                                newSystemAI.o00000(p0, p2, p3, p4)
                            }

                            override fun Ó00000(): Boolean {
                                return if (newSystemAI is com.fs.starfarer.combat.ai.system.V) { // phase ai
                                    val var1: com.fs.starfarer.combat.ai.system.V = newSystemAI
                                    var1.ôo0000().new().Õ00000()
                                } else {
                                    false
                                }
                            } // ABSOLUTELY FUCKING INSANE CODE
                            override fun o00000(): Boolean {
                                return false
                            }

                            override fun new(): ooOO? {
                                return if (newSystemAI is com.fs.starfarer.combat.ai.system.V) { // phase ai
                                    val var1: com.fs.starfarer.combat.ai.system.V = newSystemAI
                                    var1.ôo0000().new()
                                } else {
                                    null
                                }
                            }
                        }
                        set("shieldAI", shipAI, testValTwo, BasicShipAI::class.java)

                    } catch (e: Exception) {
                        SA_debugUtils.log.error("$e")
                        return
                    }


                }
                module.mutableStats.armorBonus.modifyMult(id, ARMOR_MULT)
                module.mutableStats.hullBonus.modifyMult(id, HULL_MULT)
            }
        }
    }

    fun ShipAPI.isHullDamageable(): Boolean {
        return !(this.mutableStats.getHullDamageTakenMult().getModifiedValue() <= 0.0f)
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "The removal of a shield grid leaves tremendous space for improvement, especially on something as large as a space station.",
            5f
        )

        tooltip.addPara(
            "If a module has a shield, %s it with a %s and increases armor and hull by %s and %s." +
                    "\n" +
                    "Armor is also effected, even if it doesn't have a shield.",
            5f,
            Misc.getHighlightColor(),
            "replaces", "modified damper field", toPercent((1 - ARMOR_MULT).absoluteValue), toPercent((1 - HULL_MULT).absoluteValue)
        )
        if (!SA_settings.isWindows) {
            tooltip.addPara(
                "The damper field will not appear, as you are playing on a non-windows OS. This is to prevent crashes derived " +
                        "from obfuscated symbols.",
                0f
            ).color = Misc.getGrayColor()
        }
        tooltip.addPara(
            "The damper field does not block firing of weapons.",
            5f
        ).color = Misc.getGrayColor()
    }

    override fun getBlueprintValue(): Int {
        return 4500
    }
}