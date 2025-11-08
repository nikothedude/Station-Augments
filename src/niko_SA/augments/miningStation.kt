package niko_SA.augments

import niko_SA.SA_mathUtils.trimHangingZero
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignTerrainAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Terrain
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.impl.campaign.terrain.AsteroidFieldTerrainPlugin
import com.fs.starfarer.api.impl.campaign.terrain.BaseRingTerrain
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.campaign.RingBand
import data.utilities.niko_MPC_mathUtils.roundNumTo
import exerelin.campaign.MiningHelperLegacy
import niko_SA.augments.core.stationAttachment
import niko_SR.SR_refitStationOptionAdder
import org.lazywizard.lazylib.MathUtils
import org.lazywizard.lazylib.VectorUtils
import org.lwjgl.util.vector.Vector2f
import org.magiclib.kotlin.getTerrainName
import kotlin.math.floor
import kotlin.math.roundToInt

class miningStation: stationAttachment() {

    companion object {
        val MINEABLE_TERRAIN_IDS = setOf(
            Terrain.RING,
            Terrain.ASTEROID_BELT,
            Terrain.ASTEROID_FIELD
        )
        const val CACHE_MEMID = "\$SA_miningStationTerrainValueCache"

        const val MAX_DIST = 600f
        const val MIN_DIST = 200f
        const val MINING_STRENGTH_DIVISOR = 65f

        fun getCacheForTerrain(terrain: CampaignTerrainAPI): ResourceData? {
            return terrain.memoryWithoutUpdate[CACHE_MEMID] as? ResourceData
        }
    }

    data class ResourceData(
        val resourceIds: HashMap<String, Float>,
        val outputCoeff: Float,
    ) {
        fun getMaxMiningStrengthUsable(): Float {
            val sum = resourceIds.values.sum()
            val adjusted = sum * outputCoeff

            return (adjusted * MINING_STRENGTH_DIVISOR) // arbitrary
        }
        fun getOutputEffectiveness(strength: Float): Float {
            val max = getMaxMiningStrengthUsable()
            return (strength / max).coerceAtMost(1f)
        }
        fun getOutput(): MutableMap<String, Float> {
            val output = HashMap<String, Float>()

            for (entry in resourceIds) {
                val commId = entry.key
                val value = (entry.value * outputCoeff)

                output[commId] = value
            }

            return output
        }
    }

    override fun applyInCombat(station: ShipAPI) {
        return
    }

    override fun apply() {
        super.apply()

        val ind = getStationIndustry() ?: return
        if (!ind.isFunctional) return

        val output = getOutput()
        for (entry in output) {
            var amount = entry.value
            if (amount < 0f) continue
            val commId = entry.key
            if (amount == 1) amount++ // why do i have to do this

            ind.supply("${id}_$commId", commId, amount, getName())
        }
    }

    override fun unapply() {
        super.unapply()
        val ind = getStationIndustry() ?: return
        for (supply in ind.allSupply) {
            supply.quantity.unmodify(id)
            supply.quantity.unmodify("${id}_${Commodities.RARE_ORE}")
            supply.quantity.unmodify("${id}_${Commodities.ORE}")
            supply.quantity.unmodify("${id}_${Commodities.VOLATILES}")
        }
    }

    fun getOutput(): MutableMap<String, Int> {
        val output = HashMap<String, Float>()
        output[Commodities.VOLATILES] = 0f
        output[Commodities.ORE] = 0f
        output[Commodities.RARE_ORE] = 0f
        //output[Commodities.VOLATILES] = 0

        val values = getAdjustedTerrainValues()
        val miningStrength = getMiningStrength()

        for (entry in values) {
            val terrain = entry.key
            val data = entry.value.first
            val distMult = entry.value.second

            val outputEffectiveness = data.getOutputEffectiveness(miningStrength)
            val iterOutput = data.getOutput()

            for (outputEntry in iterOutput.toMap()) {
                val existing = output[outputEntry.key]!!
                output[outputEntry.key] = existing + ((outputEntry.value * distMult) * outputEffectiveness)
            }
        }

        val finalOutput = HashMap<String, Int>()
        for (entry in output) {
            finalOutput[entry.key] = entry.value.roundToInt()
        }

        return finalOutput
    }

    fun getAllTerrainValues(): MutableMap<CampaignTerrainAPI, ResourceData> {
        val available = HashMap<CampaignTerrainAPI, ResourceData>()
        for (terrain in getMineableTerrain()) {
            val cached = getCacheForTerrain(terrain)
            if (cached != null) {
                available[terrain] = cached
                continue
            }
            // generate!
            val new = generateResourcesForTerrain(terrain)
            terrain.memoryWithoutUpdate[CACHE_MEMID] = new
            available[terrain] = new
        }

        return available
    }

    fun getDistMult(terrain: CampaignTerrainAPI, dist: Float): Float {
        val dist = dist.coerceAtLeast(0f)
        val adjustedDist = (dist - MIN_DIST).coerceAtLeast(0f)
        val adjustedMax = (MAX_DIST - MIN_DIST)
        if (adjustedDist > adjustedMax) return 0f
        val mult = 1 - (1 / (adjustedMax / adjustedDist))

        return mult.coerceAtMost(1f).coerceAtLeast(0f)
    }

    fun getAdjustedTerrainValues(): MutableMap<CampaignTerrainAPI, Pair<ResourceData, Float>> {
        val available = HashMap<CampaignTerrainAPI, Pair<ResourceData, Float>>()
        val base = getAllTerrainValues()

        val entity = getStationCampaignEntity() ?: return available
        for (entry in base) {
            val terrain = entry.key
            val casted = terrain.plugin as BaseRingTerrain
            val rad = casted.params.middleRadius
            val radius = casted.params.bandWidthInEngine
            val target = MathUtils.getPointOnCircumference(
                terrain.location,
                rad,
                VectorUtils.getAngle(terrain.location, entity.location)
            )
            var dist = MathUtils.getDistance(entity.location, target)
            dist -= entity.radius
            dist -= radius
            val mult = getDistMult(terrain, dist)

            available[terrain] = Pair(entry.value, mult)
        }

        return available
    }

    private fun generateResourcesForTerrain(terrain: CampaignTerrainAPI): ResourceData {
        val baseCommodities = HashMap<String, Float>()

        val plugin = terrain.plugin
        if (plugin is BaseRingTerrain) {
            // get type via visual
            val params = (plugin.params)
            val ringBand = params.relatedEntity as? RingBand
            val key = ringBand?.spriteKey

            if (key == null) {
                baseCommodities[Commodities.ORE] = 2.6f
                baseCommodities[Commodities.RARE_ORE] = 0.9f
                baseCommodities[Commodities.VOLATILES] = 0.3f
            } else if (key.contains("ice")) {
                baseCommodities[Commodities.VOLATILES] = 3f
                baseCommodities[Commodities.ORE] = 0.7f
                baseCommodities[Commodities.RARE_ORE] = 0.3f
            } else if (key.contains("asteroids")) {
                baseCommodities[Commodities.ORE] = 2.6f
                baseCommodities[Commodities.RARE_ORE] = 0.9f
                baseCommodities[Commodities.VOLATILES] = 0.3f
            } else if (key.contains("dust")) {
                baseCommodities[Commodities.ORE] = 1f // meh.
            }
        }

        for (entry in baseCommodities.toMap()) {
            // variation
            baseCommodities[entry.key] = entry.value * MathUtils.getRandomNumberInRange(0f, 2f)
        }

        return ResourceData(baseCommodities, getOutputCoeff(terrain))
    }

    private fun getOutputCoeff(terrain: CampaignTerrainAPI): Float {
        return MathUtils.getRandomNumberInRange(0.8f, 3f) // TODO
    }

    fun getMineableTerrain(): MutableList<CampaignTerrainAPI> {
        val mineable = ArrayList<CampaignTerrainAPI>()

        val containing = market?.containingLocation ?: return mineable
        for (terrain in containing.terrainCopy) {
            val plugin = terrain.plugin ?: continue
            if (plugin.spec.id !in MINEABLE_TERRAIN_IDS) continue
            mineable += terrain
        }

        return mineable
    }

    // requires nex in the csv, so we dont have to check
    fun getMiningStrength(): Float {
        val fleet = getStationFleet() ?: return 0f
        val member = fleet.membersWithFightersCopy.first() ?: return 0f
        val variantStore = SR_refitStationOptionAdder.getAllVariants(fleet, member)

        var total = 0f
        for (variant in variantStore) {
            val strength = MiningHelperLegacy.getVariantMiningStrength(variant)
            total += strength
        }
        return total
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Refits the station for use as a mining operation. Increases output of %s based on %s, %s and %s.",
            5f,
            Misc.getHighlightColor(),
            "stellar material", "mining weaponry", "local terrain", "distance from said terrain"
        )

        if (!Global.getSettings().isShowingCodex) {
            val strength = getMiningStrength()
            tooltip.addPara(
                "Current mining strength: %s",
                5f,
                Misc.getHighlightColor(),
                "${strength.roundToInt()}%"
            )

            val terrainValues = getAdjustedTerrainValues()
            if (terrainValues.isNotEmpty()) {
                tooltip.addPara("Nearby minable terrain: ", 5f)
                tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
                for (entry in terrainValues) {
                    val terrain = entry.key
                    val data = entry.value.first
                    val distMult = entry.value.second

                    val name = terrain.plugin.terrainName

                    if (distMult > 0f) {
                        tooltip.addPara(
                            "$name: %s density, %s extraction capability, %s effectiveness due to distance",
                            0f,
                            Misc.getHighlightColor(),
                            "${(data.outputCoeff * 100f).roundToInt()}%",
                            "${floor(data.getOutputEffectiveness(strength) * 100f).trimHangingZero()}%",
                            "${(distMult * 100f).roundToInt()}%"
                        )
                        tooltip.setBulletedListMode("   ${BaseIntelPlugin.BULLET}")
                        for (output in data.getOutput()) {
                            val spec = Global.getSettings().getCommoditySpec(output.key)
                            tooltip.addPara(
                                "%s: %s",
                                0f,
                                Misc.getHighlightColor(),
                                spec.name, "${output.value.roundNumTo(1).trimHangingZero()}%"
                            )
                        }
                        tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
                    }
                }
                tooltip.setBulletedListMode(null)
            } else {
                tooltip.addPara(
                    "There is no nearby terrain that can be mined.",
                    5f
                ).color = Misc.getNegativeHighlightColor()
            }
        }

        tooltip.addPara(
            "Mining strength is determined by station-mounted mining weaponry and strikecraft. Consider using mining lasers and mining blasters.",
            5f
        ).color = Misc.getGrayColor()
    }
}