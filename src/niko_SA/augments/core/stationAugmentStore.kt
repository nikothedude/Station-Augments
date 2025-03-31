package niko_SA.augments.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.impl.campaign.ids.Items
import niko_SA.SA_debugUtils
import niko_SA.SA_ids
import niko_SA.SA_ids.SA_augmentDefCsvPath
import niko_SA.codex.CodexData
import niko_SA.niko_SA_modPlugin
import org.lazywizard.lazylib.ext.json.iterator

object stationAugmentStore {
    val factionCSV = Global.getSettings().getMergedSpreadsheetData("faction", "data/world/factions/factions.csv")
    val factionsToTags = genFactionsToTags()

    fun genFactionsToTags(): HashMap<String, MutableSet<String>> {
        val map = HashMap<String, MutableSet<String>>()

        for (obj in factionCSV) {
            val tags = HashSet<String>()
            val sourcePath = obj.getString("faction") ?: continue // gets the path where all instances of this faction should be

            SA_debugUtils.log.info("loading ${sourcePath}")
            val mergedJson = Global.getSettings().getMergedJSON(sourcePath)
            var id: String? = null
            if (mergedJson.has("id")) {
                id = mergedJson.getString("id")
            }
            if (id != null && mergedJson.has("knownHullMods")) {
                val hmodObj = mergedJson.getJSONObject("knownHullMods")
                if (hmodObj.has("tags")) {
                    val tagsArray = hmodObj.getJSONArray("tags")
                    for (i in 0 until tagsArray.length()) {
                        val tag = tagsArray.get(i).toString()
                        tags += tag
                    }
                }
            }
            if (id != null) {
                map[id] = tags
            }
        }
        return map
    }

    fun getPlayerKnownAugments(): MutableSet<String> {
        return Global.getSector().playerFaction.getKnownAugments()
    }

    @JvmStatic
    fun FactionAPI.getKnownAugments(): MutableSet<String> {
        var knownAugments = memoryWithoutUpdate[SA_ids.SA_knownAugmentsMemFlag] as? HashSet<String>
        if (knownAugments == null) {
            val newList = HashSet<String>()
            memoryWithoutUpdate[SA_ids.SA_knownAugmentsMemFlag] = newList
            knownAugments = newList
            setupDefaultAugments(knownAugments)
        }
        return knownAugments
    }

    @JvmStatic
    fun FactionAPI.teachAugment(id: String, withCodexUpdate: Boolean = this.isPlayerFaction) {
        getKnownAugments() += id
        if (withCodexUpdate) {
            CodexData.unlockAugment(id)
        }
    }

    private fun FactionAPI.setupDefaultAugments(knownAugments: MutableSet<String>) {
        val ourHullmodTags = getHullmodTags()
        for (augment in allAugments) {
            val data = augment.value
            if (data.knowledgeTags.contains(Items.TAG_BASE_BP)/* || (data.tags.contains("standard"))*/ || (data.knowledgeTags.contains(this.id))) {
                knownAugments += augment.key
                continue
            }
            for (tag in ourHullmodTags) {
                if (data.knowledgeTags.contains(tag)) {
                    knownAugments += augment.key
                    continue
                }
            }
        }
    }

    fun FactionAPI.getHullmodTags(): MutableSet<String> {
        if (factionsToTags[id] == null) return HashSet()
        return factionsToTags[id]!!
    }

    /** The global store of all augments in the game. Make sure to modify this if adding a new augment.
     * If youre looking to add an augment as a third-party mod author, you can modify this on application load. */
    /*@JvmStatic*/ // for some reason, this makse it near impossible for this to be accessed by mods, just use the getter
    val allAugments = HashMap<String, stationAugmentSpec>()
    @JvmStatic
    fun allAugmentsExternalGetter(): HashMap<String, stationAugmentSpec> = allAugments

    fun loadAugmentsFromCSV() {
        val csv = Global.getSettings().getMergedSpreadsheetDataForMod("id", SA_augmentDefCsvPath, niko_SA_modPlugin.modId)

        for (index in 0 until csv.length())
        {
            val row = csv.getJSONObject(index)

            val id = row.getString("id")
            if (id.startsWith("#") || id == "") continue
            val knowledgeTags = row.getString("knowledge_tags").split(Regex("(, *)")).toMutableSet()
            val usageTags = row.getString("usage_tags").split(Regex("(, *)")).toMutableSet()
            val codexTags = row.getString("codex_tags").split(Regex("(, *)")).toMutableSet()
            val manufacturer = row.getString("manufac")
            val name = row.getString("name")
            val pluginPath = row.getString("plugin")
            val dropWeight = row.getDouble("drop_weight").toFloat()
            val sellWeight = row.getDouble("sell_weight").toFloat()
            val dropCombatWeight = row.getDouble("combat_drop_chance").toFloat()
            //val dropGroup = row.getString("dropGroup")
            val spritePath = row.getString("sprite_path")
            Global.getSettings().loadTexture(spritePath)
            val apCost = row.getDouble("ap_cost").toFloat()

            val spec = stationAugmentSpec(
                id,
                name,
                knowledgeTags,
                usageTags,
                codexTags,
                manufacturer,
                pluginPath,
                dropWeight,
                dropCombatWeight,
                sellWeight,
                spritePath,
                apCost
            )
            allAugments[id] = spec
        }
    }

    /*init {

        allAugments["SA_shieldShunt"] = stationAugmentSpec(
            { market: MarketAPI? -> shieldShunt(market, "SA_shieldShunt") },
            hashSetOf(Factions.HEGEMONY, Factions.LUDDIC_CHURCH),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )

        allAugments["SA_regenerativeDrones"] = stationAugmentSpec(
            { market: MarketAPI? -> regenerativeDrones(market, "SA_regenerativeDrones") },
            hashSetOf(Factions.TRITACHYON, Factions.HEGEMONY, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )

        allAugments["SA_axialOverclocking"] = stationAugmentSpec(
            { market: MarketAPI? -> axialOverclocking(market, "SA_axialOverclocking") },
            hashSetOf(Factions.PIRATES, Factions.PERSEAN, Factions.HEGEMONY, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_safetyOverrides"] = stationAugmentSpec(
            { market: MarketAPI? -> safetyOverrides(market, "SA_safetyOverrides") },
            hashSetOf(SA_ids.ALL_FACTIONS),
            mutableMapOf(Pair("SA_augmentNormal", 2f)),
            0.5f
        )
        allAugments["SA_bubbleShield"] = stationAugmentSpec(
            { market: MarketAPI? -> bubbleShield(market, "SA_bubbleShield") },
            hashSetOf(Factions.TRITACHYON),
            mutableMapOf(Pair("SA_augmentRare", 10f)),
            0.1f
        )
        /*allAugments["SA_defenseGarrison"] = stationAugmentData(
            { market: MarketAPI? -> defenseGarrison(market, "SA_defenseGarrison") },
            true,
            mutableMapOf(Pair("SA_augmentNormal", 2f)
            )
        )*/
        allAugments["SA_fighterTimeflow"] = stationAugmentSpec(
            { market: MarketAPI? -> fighterTimeflow(market, "SA_fighterTimeflow") },
            hashSetOf(Factions.TRITACHYON),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        /*allAugments["SA_droneAAF"] = stationAugmentData(
            { market: MarketAPI? -> droneAAF(market, "SA_droneAAF") },
            HashSet<String
(),            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )*/
        allAugments["SA_supportOutfit"] = stationAugmentSpec(
            { market: MarketAPI? -> supportOutfit(market, "SA_supportOutfit") },
            hashSetOf(Factions.HEGEMONY),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_fluxShunt"] = stationAugmentSpec(
            { market: MarketAPI? -> fluxShunt(market, "SA_fluxShunt") },
            hashSetOf(Factions.PERSEAN),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_heavyArmor"] = stationAugmentSpec(
            { market: MarketAPI? -> heavyArmor(market, "SA_heavyArmor") },
            hashSetOf(Factions.HEGEMONY, Factions.LUDDIC_CHURCH, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_solarShielding"] = stationAugmentSpec(
            { market: MarketAPI? -> solarShielding(market, "SA_solarShielding") },
            hashSetOf(Factions.DIKTAT),
            mutableMapOf(Pair("SA_augmentNormal", 9f))
        )
        allAugments["SA_aiFighterUplink"] = stationAugmentSpec(
            { market: MarketAPI? -> aiFighterUplink(market, "SA_aiFighterUplink") },
            hashSetOf(Factions.TRITACHYON),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_highExplosive"] = stationAugmentSpec(
            { market: MarketAPI? -> highExplosive(market, "SA_highExplosive") },
            hashSetOf(Factions.PIRATES, Factions.LUDDIC_PATH, Factions.DIKTAT, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_stabilizedShields"] = stationAugmentSpec(
            { market: MarketAPI? -> stabilizedShields(market, "SA_stabilizedShields") },
            hashSetOf(Factions.TRITACHYON, Factions.HEGEMONY, Factions.INDEPENDENT, Factions.PERSEAN),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_automatedRepairUnit"] = stationAugmentSpec(
            { market: MarketAPI? -> automatedRepairUnit(market, "SA_automatedRepairUnit") },
            hashSetOf(Factions.HEGEMONY, Factions.LUDDIC_CHURCH, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_ECCMPackage"] = stationAugmentSpec(
            { market: MarketAPI? -> ECCMPackage(market, "SA_ECCMPackage") },
            hashSetOf(Factions.TRITACHYON, Factions.HEGEMONY),
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_commsCenter"] = stationAugmentSpec(
            { market: MarketAPI? -> commsCenter(market, "SA_commsCenter") },
            hashSetOf(Factions.TRITACHYON, Factions.HEGEMONY, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_ECMPackage"] = stationAugmentSpec(
            { market: MarketAPI? -> ECMPackage(market, "SA_ECMPackage") },
            hashSetOf(Factions.TRITACHYON, Factions.HEGEMONY, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_navRelay"] = stationAugmentSpec(
            { market: MarketAPI? -> navRelay(market, "SA_navRelay") },
            hashSetOf(Factions.HEGEMONY, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_industryConversion"] = stationAugmentSpec(
            { market: MarketAPI? -> industryConversion(market, "SA_industryConversion") },
            HashSet<String>(),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_resistantFluxConduits"] = stationAugmentSpec(
            { market: MarketAPI? -> resistantFluxConduits(market, "SA_resistantFluxConduits") },
            hashSetOf(Factions.HEGEMONY, Factions.TRITACHYON, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_reinforcedBulkheads"] = stationAugmentSpec(
            { market: MarketAPI? -> reinforcedBulkheads(market, "SA_reinforcedBulkheads") },
            hashSetOf(SA_ids.ALL_FACTIONS),
            mutableMapOf(Pair("SA_augmentNormal", 2f))
        )
        allAugments["SA_armoredWeaponMounts"] = stationAugmentSpec(
            { market: MarketAPI? -> armoredWeaponMounts(market, "SA_armoredWeaponMounts") },
            hashSetOf(Factions.HEGEMONY, Factions.LUDDIC_CHURCH, Factions.INDEPENDENT, Factions.DIKTAT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_highResSensors"] = stationAugmentSpec(
            { market: MarketAPI? -> highResSensors(market, "SA_highResSensors") },
            hashSetOf(Factions.TRITACHYON, Factions.DIKTAT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_supportPackage"] = stationAugmentSpec(
            { market: MarketAPI? -> supportPackage(market, "SA_supportPackage") },
            HashSet(),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_logisticsDrones"] = stationAugmentSpec(
            { market: MarketAPI? -> logisticsDrones(market, "SA_logisticsDrones") },
            HashSet(),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_moteSink"] = stationAugmentSpec(
            { market: MarketAPI? -> moteSink(market, "SA_moteSink") },
            HashSet(),
            HashMap() // doesnt spawn naturally
        )
        allAugments["SA_moteSinkLow"] = stationAugmentSpec(
            { market: MarketAPI? -> moteSinkLow(market, "SA_moteSinkLow") },
            HashSet(),
            HashMap(), // doesnt spawn naturally,
            0f
        )
        allAugments["SA_hydroponics"] = stationAugmentSpec(
            { market: MarketAPI? -> hydroponics(market, "SA_hydroponics") },
            hashSetOf(Factions.HEGEMONY),
            mutableMapOf(Pair("SA_augmentRare", 10f)),
            0.1f
        )
        allAugments["SA_stationRepairUnit"] = stationAugmentSpec(
            { market: MarketAPI? -> stationRepairUnit(market, "SA_stationRepairUnit") },
            HashSet(),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_jumpEngine"] = stationAugmentSpec(
            { market: MarketAPI? -> jumpPointCreator(market, "SA_jumpEngine") },
            HashSet(),
            mutableMapOf(Pair("SA_augmentRare", 8f))
        )*/

        // doesnt work, DTA just. dosent work on stations
        /*allAugments["SA_defensiveTargetingArray"] = stationAugmentData(
            { market: MarketAPI? -> defensiveTargettingArray(market, "SA_defensiveTargetingArray") },
            HashSet<String
(),            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )*/

    //}

    /*/**
     * A test method used to determine if any augments have the wrong ID.
     *
     * @throws: A [RuntimeException] if any augments have the wrong ID.
     * */
    fun testIdSync() {
        for (entry in allAugments) {
            val data = entry.value
            val id = entry.key

            val instance = data.getNewPluginInstance()
            if (instance.id != id) {
                throw RuntimeException(
                    "Incorrect augment ID set on ${instance.name}! Expected ${id}, got ${instance.id}"
                )
            }
        }
    }*/
}