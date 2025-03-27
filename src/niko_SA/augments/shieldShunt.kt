package niko_SA.augments

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.ShipSystems
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.ai.*
import com.fs.starfarer.combat.ai.attack.AttackAIModule
import com.fs.starfarer.combat.entities.Ship
import com.fs.starfarer.loading.SpecStore
import niko_SA.ReflectionUtils.get
import niko_SA.ReflectionUtils.set
import niko_SA.SA_debugUtils
import niko_SA.SA_settings
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent
import org.lwjgl.util.vector.Vector2f
import kotlin.math.absoluteValue

/** FIXME: Reflection will crash on non-windows, make non-windows versions */
/** TODO: In the case of updating, check comments below to see what to change*/
class shieldShunt(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    override val manufacturer: String = "Mbaye-Gogol"
    override val name: String = "K-Type Shield Shunt"
    override val spriteId: String = "graphics/hullmods/shield_shunt.png"

    companion object {
        const val ARMOR_MULT = 1.4f
        const val HULL_MULT = 1.1f
    }

    override val augmentCost: Float = 10f

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            if (!(module as Ship).isHullDamageable) continue
            if (module.shield != null || module.hullSpec.hasTag(Tags.MODULE_HULL_BAR_ONLY)) { // armor
                if (module.shield != null) {
                    module.setShield(ShieldAPI.ShieldType.PHASE, 0f, 1f, 0f)
                }
                if (module.fluxCapacity < 5) {
                    module.mutableStats.fluxCapacity.modifyFlat(id, 5f)
                }
                if (module.mutableStats.fluxDissipation.modifiedInt < 5) {
                    module.mutableStats.fluxDissipation.modifyFlat(id, 5f)
                }

                val shipAI = module.ai
                if (SA_settings.isWindows && shipAI is BasicShipAI) { // no compatability for custom ais, sorry
                    try {
                        val damperSpec = Global.getSettings().getShipSystemSpec("SA_KKdamper") as com.fs.starfarer.loading.specs.oO0O
                        set(
                            "phaseCloak",
                            module,
                            damperSpec.createSystem(module)
                        )

                        val threatEvalAI = get("threatEvalAI", module.ai, BasicShipAI::class.java)
                        val attackAI = get("attackAI", module.ai, BasicShipAI::class.java)
                        val flockingAI = get("flockingAI", module.ai, BasicShipAI::class.java)

                        val newSystemAI = damperSpec.createSystemAI(
                            module, module.aiFlags,
                            threatEvalAI as? D,
                            attackAI as? AttackAIModule,
                            flockingAI as? com.fs.starfarer.combat.ai.movement.A,
                            module.ai as? (com.fs.starfarer.combat.ai.movement.maneuvers.`do`.o) //ShipAI obf class
                        )
                        // v mimics a anonymous wrapper the convinces the game to laod a systemai as a shieldai. see basicshipai for more, its in its constructor
                        val testValTwo = object : oOoOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO { // ABSOLUTELY FUCKING INSANE CODE
                            override fun o00000(): Boolean {
                                return false
                            }

                            override fun Ó00000(): Boolean {
                                return if (newSystemAI is com.fs.starfarer.combat.ai.system.V) { // phase ai
                                    val var1: com.fs.starfarer.combat.ai.system.V = newSystemAI
                                    var1.`while.super`().new().`class`()
                                } else {
                                    false
                                }
                            }

                            override fun new(): C? {
                                return if (newSystemAI is com.fs.starfarer.combat.ai.system.V) {
                                    val var1: com.fs.starfarer.combat.ai.system.V = newSystemAI
                                    var1.`while.super`().new()
                                } else {
                                    null
                                }
                            }

                            override fun o00000(
                                var1: Float,
                                var2: D?,
                                var3x: Vector2f?,
                                var4: Vector2f?,
                                var5: Ship?
                            ) {
                                newSystemAI.o00000(var1, var3x, var4, var5)
                            }
                        }
                        set("shieldAI", module.ai, testValTwo, BasicShipAI::class.java)

                    } catch (e: ClassNotFoundException) {
                        SA_debugUtils.log.error("incompatible starsector version!")
                        return
                    }

                }
                module.mutableStats.armorBonus.modifyMult(id, ARMOR_MULT)
                module.mutableStats.hullBonus.modifyMult(id, HULL_MULT)
            }
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean) {
        super.getBasicDescription(tooltip, expanded)

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