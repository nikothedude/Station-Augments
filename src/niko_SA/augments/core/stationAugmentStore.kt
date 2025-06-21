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

// This is transient, remember that
object stationAugmentStore {
    val factionsToTags = genFactionsToTags()

    fun genFactionsToTags(): HashMap<String, MutableSet<String>> {
        val map = HashMap<String, MutableSet<String>>()

        for (obj in Global.getSettings().getMergedSpreadsheetData("faction", "data/world/factions/factions.csv")) {
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
            if (data.knowledgeTags.contains(Items.TAG_BASE_BP) || (data.knowledgeTags.contains("standard") && !this.isPlayerFaction) || (data.knowledgeTags.contains(this.id))) {
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
            if (id.startsWith("#") || id.isEmpty()) continue
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
            var modId = row.optString("modid") ?: niko_SA_modPlugin.modId
            if (Global.getSettings().modManager.getModSpec(modId) == null) modId = niko_SA_modPlugin.modId
            var reqItemId = row.optString("req_item_id")
            if (reqItemId.isEmpty()) reqItemId = null

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
                apCost,
                reqItemId,
                modId
            )
            allAugments[id] = spec
        }
    }
}