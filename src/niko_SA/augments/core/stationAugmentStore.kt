package niko_SA.augments.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import niko_SA.SA_ids
import niko_SA.augments.*

object stationAugmentStore {
    fun getPlayerKnownAugments(): MutableSet<String> {
        return Global.getSector().playerFaction.getKnownAugments()
    }

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

    private fun setupDefaultAugments(knownAugments: MutableSet<String>) {
        for (augment in allAugments) {
            val data = augment.value
            if (data.knownToAllFactionsByDefault) {
                knownAugments += augment.key
            }
        }
    }

    /** The global store of all augments in the game. Make sure to modify this if adding a new augment.
     * If youre looking to add an augment as a third-party mod author, you can modify this on application load. */
    val allAugments = HashMap<String, stationAugmentData>()

    init {

        allAugments["SA_axialOverclocking"] = stationAugmentData(
            { market: MarketAPI? -> axialOverclocking(market, "SA_axialOverclocking") },
            false,
                mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_safetyOverrides"] = stationAugmentData(
            { market: MarketAPI? -> safetyOverrides(market, "SA_safetyOverrides") },
            true,
                mutableMapOf(Pair("SA_augmentNormal", 2f)
            )
        )
        allAugments["SA_bubbleShield"] = stationAugmentData(
            { market: MarketAPI? -> bubbleShield(market, "SA_bubbleShield") },
            false,
                mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
        allAugments["SA_defenseGarrison"] = stationAugmentData(
            { market: MarketAPI? -> defenseGarrison(market, "SA_defenseGarrison") },
            true,
            mutableMapOf(Pair("SA_augmentNormal", 2f)
            )
        )
        allAugments["SA_supportOutfit"] = stationAugmentData(
            { market: MarketAPI? -> supportOutfit(market, "SA_supportOutfit") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_regenerativeDrones"] = stationAugmentData(
            { market: MarketAPI? -> regenerativeDrones(market, "SA_regenerativeDrones") },
            false,
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
        allAugments["SA_shieldShunt"] = stationAugmentData(
            { market: MarketAPI? -> shieldShunt(market, "SA_shieldShunt") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_fluxShunt"] = stationAugmentData(
            { market: MarketAPI? -> fluxShunt(market, "SA_fluxShunt") },
            false,
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
        allAugments["SA_heavyArmor"] = stationAugmentData(
            { market: MarketAPI? -> heavyArmor(market, "SA_heavyArmor") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_solarShielding"] = stationAugmentData(
            { market: MarketAPI? -> solarShielding(market, "SA_solarShielding") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_aiFighterUplink"] = stationAugmentData(
            { market: MarketAPI? -> aiFighterUplink(market, "SA_aiFighterUplink") },
            false,
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
    }
}