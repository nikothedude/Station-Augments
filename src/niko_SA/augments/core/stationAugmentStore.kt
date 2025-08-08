package niko_SA.augments.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.impl.campaign.ids.Items
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_debugUtils
import niko_SA.SA_ids
import niko_SA.SA_ids.SA_augmentDefCsvPath
import niko_SA.SA_settings
import niko_SA.augments.autofitPlugins.StationAugmentAutofitPlugin
import niko_SA.codex.CodexData
import niko_SA.niko_SA_modPlugin
import org.lazywizard.lazylib.ext.json.iterator
import java.awt.Color

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

    /// Only use if you're calling from java.
    @JvmStatic
    fun getKnownAugmentsOfFac(faction: FactionAPI) = faction.getKnownAugments()

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
        teachAugmentExternal(this, id, withCodexUpdate)
    }

    @JvmStatic
    fun teachAugmentExternal(faction: FactionAPI, id: String, withCodexUpdate: Boolean = faction.isPlayerFaction) {
        faction.getKnownAugments() += id
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

    /** The global store of all augments in the game. */
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
            var autofitPluginPath = row.optString("autofit_plugin_path")
            var autofitPlugin: StationAugmentAutofitPlugin? = null
            if (autofitPluginPath.isNotEmpty()) {
                autofitPlugin = Global.getSettings().scriptClassLoader.loadClass(autofitPluginPath).newInstance() as StationAugmentAutofitPlugin
            }
            var nameColor: Color = Color.WHITE
            var nameColorId = row.optString("name_color")
            if (nameColorId.isEmpty()) {
                nameColorId = null
            } else {
                for (faction in Global.getSettings().allFactionSpecs) {
                    if (faction.id == nameColorId) {
                        nameColor = faction.baseUIColor
                    }
                }
            }

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
                autofitPlugin,
                nameColor,
                modId
            )
            allAugments[id] = spec
        }

        postAugmentsLoaded()
    }

    private fun postAugmentsLoaded() {
        if (SA_settings.AOTDVaultsEnabled && SA_settings.AOTDVaultsVersion >= "3.5.0") {
            allAugments["SA_shroudedMantle"]?.requiredItemId = "aotd_tenebrium"
            allAugments["SA_shroudedLens"]?.requiredItemId = "aotd_tenebrium"
            allAugments["SA_shroudedThunderhead"]?.requiredItemId = "aotd_tenebrium"
        }
    }
}