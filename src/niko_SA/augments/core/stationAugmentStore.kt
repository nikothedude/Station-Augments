package niko_SA.augments.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import niko_SA.SA_ids
import niko_SA.SA_settings.isWindows
import niko_SA.augments.*

object stationAugmentStore {
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
    /*@JvmStatic*/ // for some reason, this makse it near impossible for this to be accessed by mods, just use the getter
    val allAugments = HashMap<String, stationAugmentData>()
    @JvmStatic
    fun allAugmentsExternalGetter(): HashMap<String, stationAugmentData> = allAugments

    init {

        if (isWindows) { // these use reflection so dont work off windows
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
        }

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
        /*allAugments["SA_defenseGarrison"] = stationAugmentData(
            { market: MarketAPI? -> defenseGarrison(market, "SA_defenseGarrison") },
            true,
            mutableMapOf(Pair("SA_augmentNormal", 2f)
            )
        )*/
        allAugments["SA_fighterTimeflow"] = stationAugmentData(
            { market: MarketAPI? -> fighterTimeflow(market, "SA_fighterTimeflow") },
            false,
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
        /*allAugments["SA_droneAAF"] = stationAugmentData(
            { market: MarketAPI? -> droneAAF(market, "SA_droneAAF") },
            false,
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )*/
        allAugments["SA_supportOutfit"] = stationAugmentData(
            { market: MarketAPI? -> supportOutfit(market, "SA_supportOutfit") },
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
            mutableMapOf(Pair("SA_augmentNormal", 9f)
            )
        )
        allAugments["SA_aiFighterUplink"] = stationAugmentData(
            { market: MarketAPI? -> aiFighterUplink(market, "SA_aiFighterUplink") },
            false,
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
        allAugments["SA_highExplosive"] = stationAugmentData(
            { market: MarketAPI? -> highExplosive(market, "SA_highExplosive") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_stabilizedShields"] = stationAugmentData(
            { market: MarketAPI? -> stabilizedShields(market, "SA_stabilizedShields") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_automatedRepairUnit"] = stationAugmentData(
            { market: MarketAPI? -> automatedRepairUnit(market, "SA_automatedRepairUnit") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_ECCMPackage"] = stationAugmentData(
            { market: MarketAPI? -> ECCMPackage(market, "SA_ECCMPackage") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_commsCenter"] = stationAugmentData(
            { market: MarketAPI? -> commsCenter(market, "SA_commsCenter") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_ECMPackage"] = stationAugmentData(
            { market: MarketAPI? -> ECMPackage(market, "SA_ECMPackage") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_navRelay"] = stationAugmentData(
            { market: MarketAPI? -> navRelay(market, "SA_navRelay") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_industryConversion"] = stationAugmentData(
            { market: MarketAPI? -> industryConversion(market, "SA_industryConversion") },
            false,
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
        allAugments["SA_resistantFluxConduits"] = stationAugmentData(
            { market: MarketAPI? -> resistantFluxConduits(market, "SA_resistantFluxConduits") },
            false,
            mutableMapOf(Pair("SA_augmentRare", 10f)
            )
        )
        allAugments["SA_reinforcedBulkheads"] = stationAugmentData(
            { market: MarketAPI? -> reinforcedBulkheads(market, "SA_reinforcedBulkheads") },
            true,
            mutableMapOf(Pair("SA_augmentNormal", 2f)
            )
        )
        allAugments["SA_armoredWeaponMounts"] = stationAugmentData(
            { market: MarketAPI? -> armoredWeaponMounts(market, "SA_armoredWeaponMounts") },
            false,
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )

    }
}