package niko_SA.augments

import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.*
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.combat.ai.C
import com.fs.starfarer.combat.ai.O0OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO
import com.fs.starfarer.combat.ai.attack.AttackAIModule
import com.fs.starfarer.combat.ai.movement.maneuvers.oO0O
import com.fs.starfarer.combat.ai.movement.oOOO
import com.fs.starfarer.combat.entities.Ship
import com.fs.starfarer.loading.SpecStore
import com.fs.starfarer.loading.specs.M
import niko_SA.ReflectionUtils.get
import niko_SA.ReflectionUtils.set
import niko_SA.augments.core.stationAttachment
import niko_SA.stringUtils.toPercent
import org.lwjgl.util.vector.Vector2f
import kotlin.math.absoluteValue

/** FIXME: Reflection will crash on non-windows, make non-windows versions */
/** TODO: In the case of updating, check comments below to see what to change*/
class shieldShunt(market: MarketAPI?, id: String) : stationAttachment(market, id) {

    override val manufacturer: String = "Mbaye-Gogol"
    override val name: String = "K-Type Shield Shunt"
    override val spriteId: String = "graphics/icons/industry/mining.png"

    companion object {
        const val ARMOR_MULT = 1.5f
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
                if (shipAI != null) {
                    val testVal: M = SpecStore.o00000(M::class.java, "damper") // EVIL FUCKED UP CODE
                    set(
                        "phaseCloak",
                        module,
                        testVal.createSystem(module)
                    )
                    val threatEvalAI = get("threatEvalAI", module.ai)
                    val attackAI = get("attackAI", module.ai)
                    val flockingAI = get("flockingAI", module.ai)

                    val newSystemAI = testVal.createSystemAI(
                        module, module.aiFlags,
                        threatEvalAI as O0OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO?,
                        attackAI as AttackAIModule?,
                        flockingAI as oOOO?,
                        module.ai as (oO0O.o) //ShipAPI obf class
                    )
                    // v mimics a anonymous wrapper the convinces the game to laod a systemai as a shieldai. see basicshipai for more, its in its constructor
                    val testValTwo = object : com.fs.starfarer.combat.ai.G { // ABSOLUTELY FUCKING INSANE CODE
                        override fun o00000(): Boolean {
                            return false
                        }

                        override fun Object(): Boolean {
                            return if (newSystemAI is com.fs.starfarer.combat.ai.system.V) { // phase ai
                                val var1: com.fs.starfarer.combat.ai.system.V = newSystemAI
                                var1.ôo0000().Ò00000().Õ00000()
                            } else {
                                false
                            }
                        }

                        override fun Ò00000(): C? {
                            return if (newSystemAI is com.fs.starfarer.combat.ai.system.V) {
                                val var1: com.fs.starfarer.combat.ai.system.V = newSystemAI
                                var1.ôo0000().Ò00000()
                            } else {
                                null
                            }
                        }

                        override fun o00000(
                            var1: Float,
                            var2: O0OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO?,
                            var3x: Vector2f?,
                            var4: Vector2f?,
                            var5: Ship?
                        ) {
                            newSystemAI.`super`(var1, var3x, var4, var5)
                        }
                    }
                    set("shieldAI", module.ai, testValTwo)

                    module.mutableStats.armorBonus.modifyMult(id, ARMOR_MULT)
                    module.mutableStats.hullBonus.modifyMult(id, HULL_MULT)
                }
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
            "replaces", "damper field", toPercent((1 - ARMOR_MULT).absoluteValue), toPercent((1 - HULL_MULT).absoluteValue)
        )
    }
}