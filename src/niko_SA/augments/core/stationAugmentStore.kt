package niko_SA.augments.core

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.FactionAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
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

    private fun FactionAPI.setupDefaultAugments(knownAugments: MutableSet<String>) {
        for (augment in allAugments) {
            val data = augment.value
            if (data.factionsThatKnowByDefault.contains(SA_ids.ALL_FACTIONS) || (data.factionsThatKnowByDefault.contains(this.id))) {
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
                hashSetOf(Factions.TRITACHYON, Factions.HEGEMONY, Factions.INDEPENDENT),
                mutableMapOf(Pair("SA_augmentRare", 10f))
            )
            allAugments["SA_shieldShunt"] = stationAugmentData(
                { market: MarketAPI? -> shieldShunt(market, "SA_shieldShunt") },
                hashSetOf(Factions.HEGEMONY, Factions.LUDDIC_CHURCH),
                mutableMapOf(Pair("SA_augmentNormal", 10f))
            )
        }

        allAugments["SA_axialOverclocking"] = stationAugmentData(
            { market: MarketAPI? -> axialOverclocking(market, "SA_axialOverclocking") },
            hashSetOf(Factions.PIRATES, Factions.PERSEAN, Factions.HEGEMONY, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_safetyOverrides"] = stationAugmentData(
            { market: MarketAPI? -> safetyOverrides(market, "SA_safetyOverrides") },
            hashSetOf(SA_ids.ALL_FACTIONS),
            mutableMapOf(Pair("SA_augmentNormal", 2f)),
            0.5f
        )
        allAugments["SA_bubbleShield"] = stationAugmentData(
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
        allAugments["SA_fighterTimeflow"] = stationAugmentData(
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
        allAugments["SA_supportOutfit"] = stationAugmentData(
            { market: MarketAPI? -> supportOutfit(market, "SA_supportOutfit") },
            hashSetOf(Factions.HEGEMONY),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_fluxShunt"] = stationAugmentData(
            { market: MarketAPI? -> fluxShunt(market, "SA_fluxShunt") },
            hashSetOf(Factions.PERSEAN),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_heavyArmor"] = stationAugmentData(
            { market: MarketAPI? -> heavyArmor(market, "SA_heavyArmor") },
            hashSetOf(Factions.HEGEMONY, Factions.LUDDIC_CHURCH, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_solarShielding"] = stationAugmentData(
            { market: MarketAPI? -> solarShielding(market, "SA_solarShielding") },
            hashSetOf(Factions.DIKTAT),
            mutableMapOf(Pair("SA_augmentNormal", 9f))
        )
        allAugments["SA_aiFighterUplink"] = stationAugmentData(
            { market: MarketAPI? -> aiFighterUplink(market, "SA_aiFighterUplink") },
            hashSetOf(Factions.TRITACHYON),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_highExplosive"] = stationAugmentData(
            { market: MarketAPI? -> highExplosive(market, "SA_highExplosive") },
            hashSetOf(Factions.PIRATES, Factions.LUDDIC_PATH, Factions.DIKTAT, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_stabilizedShields"] = stationAugmentData(
            { market: MarketAPI? -> stabilizedShields(market, "SA_stabilizedShields") },
            hashSetOf(Factions.TRITACHYON, Factions.HEGEMONY, Factions.INDEPENDENT, Factions.PERSEAN),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_automatedRepairUnit"] = stationAugmentData(
            { market: MarketAPI? -> automatedRepairUnit(market, "SA_automatedRepairUnit") },
            hashSetOf(Factions.HEGEMONY, Factions.LUDDIC_CHURCH, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_ECCMPackage"] = stationAugmentData(
            { market: MarketAPI? -> ECCMPackage(market, "SA_ECCMPackage") },
            hashSetOf(Factions.TRITACHYON, Factions.HEGEMONY),
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_commsCenter"] = stationAugmentData(
            { market: MarketAPI? -> commsCenter(market, "SA_commsCenter") },
            hashSetOf(Factions.TRITACHYON, Factions.HEGEMONY, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f)
            )
        )
        allAugments["SA_ECMPackage"] = stationAugmentData(
            { market: MarketAPI? -> ECMPackage(market, "SA_ECMPackage") },
            hashSetOf(Factions.TRITACHYON, Factions.HEGEMONY, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_navRelay"] = stationAugmentData(
            { market: MarketAPI? -> navRelay(market, "SA_navRelay") },
            hashSetOf(Factions.HEGEMONY, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_industryConversion"] = stationAugmentData(
            { market: MarketAPI? -> industryConversion(market, "SA_industryConversion") },
            HashSet<String>(),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_resistantFluxConduits"] = stationAugmentData(
            { market: MarketAPI? -> resistantFluxConduits(market, "SA_resistantFluxConduits") },
            hashSetOf(Factions.HEGEMONY, Factions.TRITACHYON, Factions.INDEPENDENT),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_reinforcedBulkheads"] = stationAugmentData(
            { market: MarketAPI? -> reinforcedBulkheads(market, "SA_reinforcedBulkheads") },
            hashSetOf(SA_ids.ALL_FACTIONS),
            mutableMapOf(Pair("SA_augmentNormal", 2f))
        )
        allAugments["SA_armoredWeaponMounts"] = stationAugmentData(
            { market: MarketAPI? -> armoredWeaponMounts(market, "SA_armoredWeaponMounts") },
            hashSetOf(Factions.HEGEMONY, Factions.LUDDIC_CHURCH, Factions.INDEPENDENT, Factions.DIKTAT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_highResSensors"] = stationAugmentData(
            { market: MarketAPI? -> highResSensors(market, "SA_armoredWeaponMounts") },
            hashSetOf(Factions.TRITACHYON, Factions.DIKTAT),
            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )
        allAugments["SA_supportPackage"] = stationAugmentData(
            { market: MarketAPI? -> supportPackage(market, "SA_supportPackage") },
            HashSet<String>(),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_logisticsDrones"] = stationAugmentData(
            { market: MarketAPI? -> logisticsDrones(market, "SA_logisticsDrones") },
            HashSet<String>(),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        allAugments["SA_moteSink"] = stationAugmentData(
            { market: MarketAPI? -> moteSink(market, "SA_moteSink") },
            HashSet(),
            HashMap() // doesnt spawn naturally
        )
        allAugments["SA_moteSinkLow"] = stationAugmentData(
            { market: MarketAPI? -> moteSinkLow(market, "SA_moteSinkLow") },
            HashSet<String>(),
            HashMap(), // doesnt spawn naturally,
            0f
        )
        allAugments["SA_hydroponics"] = stationAugmentData(
            { market: MarketAPI? -> hydroponics(market, "SA_hydroponics") },
            hashSetOf(Factions.HEGEMONY),
            mutableMapOf(Pair("SA_augmentRare", 10f)),
            0.1f
        )
        allAugments["SA_stationRepairUnit"] = stationAugmentData(
            { market: MarketAPI? -> stationRepairUnit(market, "SA_stationRepairUnit") },
            HashSet(),
            mutableMapOf(Pair("SA_augmentRare", 10f))
        )
        // doesnt work, DTA just. dosent work on stations
        /*allAugments["SA_defensiveTargetingArray"] = stationAugmentData(
            { market: MarketAPI? -> defensiveTargettingArray(market, "SA_defensiveTargetingArray") },
            HashSet<String
(),            mutableMapOf(Pair("SA_augmentNormal", 10f))
        )*/

    }
}