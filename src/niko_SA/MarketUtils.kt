package niko_SA

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.Pair
import niko_SA.SA_settings.BASE_STATION_AUGMENT_BUDGET
import niko_SA.augments.core.BuiltInMode
import niko_SA.augments.core.stationAttachment
import niko_SA.augments.core.stationAttachment.Companion.BEST_OF_THE_BEST_AP_BONUS
import niko_SA.augments.core.stationAttachment.Companion.STATION_IMPROVED_AP_BONUS
import niko_SA.augments.core.stationAugmentStore

object MarketUtils {
    /** Returns the augment budget this station has. An augment budget controls how many augments a station can have - each augment has its own cost.*/
    @JvmStatic
    fun OrbitalStation.getAugmentBudget(): Float {
        var points = BASE_STATION_AUGMENT_BUDGET
        if (isImproved) points += STATION_IMPROVED_AP_BONUS

        for (tag in spec.tags) {
            val tagBonus = stationAttachment.tagToExtraAugmentBudget[tag]
            if (tagBonus != null) {
                points += tagBonus
            }
        }
        val marketBonus = market?.memoryWithoutUpdate?.getFloat(SA_ids.MARKET_BONUS_BUDGET_MEMID) ?: 0f
        points += marketBonus

        if (market.isPlayerOwned) {
            val playerPerson = Global.getSector().playerPerson
            if (playerPerson.stats.hasSkill(Skills.BEST_OF_THE_BEST)) {
                points += BEST_OF_THE_BEST_AP_BONUS
            }
        }

        return points
    }

    @JvmStatic
    fun MarketAPI.getUsedAugmentBudget(): Float {
        var used = 0f

        for (augment in getStationAugments()) {
            if (augment.builtInMode == BuiltInMode.NOT) {
                used += augment.getAugmentCost()
            }
        }

        return used
    }

    @JvmStatic
    fun OrbitalStation.getUsedAugmentBudget(): Float {
        return market.getUsedAugmentBudget()
    }

    @JvmStatic
    fun OrbitalStation.getRemainingAugmentBudget(): Float {
        val budget = getAugmentBudget()
        val usedBudget = getUsedAugmentBudget()

        return (budget - usedBudget)
    }

    @JvmStatic
    fun MarketAPI.getRemainingAugmentBudget(): Float {
        val stationIndustry = (getStationIndustry() as? OrbitalStation) ?: return 0f
        return stationIndustry.getRemainingAugmentBudget()
    }

    @JvmStatic
    fun MarketAPI.getStationAugments(): MutableSet<stationAttachment> {
        var stationAugments: MutableSet<stationAttachment>? = memoryWithoutUpdate[SA_ids.SA_stationAugmentListMemId] as? MutableSet<stationAttachment>
        if (stationAugments == null) {
            val newList = HashSet<stationAttachment>()
            memoryWithoutUpdate[SA_ids.SA_stationAugmentListMemId] = newList
            stationAugments = newList
        }

        return stationAugments
    }

    /*fun MarketAPI.addStationAugment(augment: stationAttachment) {
        getStationAugments() += augment
        augment.applied()
    }*/

    @JvmStatic
    fun MarketAPI.toggleStationAugment(instance: stationAttachment, checkForStation: Boolean = true) {
        if (instance.applied) {
            removeStationAugment(instance)
        } else {
            addStationAugment(instance, checkForStation)
        }
    }

    @JvmStatic
    fun MarketAPI.addStationAugment(id: String, checkForStation: Boolean = true): stationAttachment? {
        val augment = stationAugmentStore.allAugments[id]?.getNewPluginInstance(this) ?: return null
        return addStationAugment(augment, checkForStation)
    }

    @JvmStatic
    fun MarketAPI.addStationAugment(augment: stationAttachment, checkForStation: Boolean = true): stationAttachment? {
        if (hasStationAugment(augment)) {
            SA_debugUtils.log.warn("tried to add ${augment.id} while $name already had it!")
            return null
        }
        if (checkForStation) {
            val industry = getStationIndustry()
            if (industry == null) {
                SA_debugUtils.log.info("$name has no station, aborting addition of ${augment.id}")
                return null
            }
        }
        getStationAugments() += augment
        augment.onAdded()

        return augment
    }

    @JvmStatic
    fun MarketAPI.removeStationAugment(id: String) {
        val augment = getStationAugments().firstOrNull { it.id == id } ?: return

        removeStationAugment(augment)
    }

    @JvmStatic
    fun MarketAPI.removeStationAugment(augment: stationAttachment) {
        getStationAugments() -= augment
        augment.onRemoved()
    }

    @JvmStatic
    fun MarketAPI.getStationIndustry(): Industry? {
        return Misc.getStationIndustry(this)
    }

    @JvmStatic
    fun MarketAPI.hasStationAugment(augment: stationAttachment): Boolean {
        return hasStationAugment(augment.id)
    }

    @JvmStatic
    fun MarketAPI.hasStationAugment(augmentId: String): Boolean {
        return getStationAugments().any { it.id == augmentId }
    }

    @JvmStatic
    fun MarketAPI.hasFragmentSwarm(): Boolean {
        return hasStationAugment("SA_fragmentSwarm")
    }

    // TODO: update this method if it ever changes, 1:1 with applyDeficitToProduction from baseindustry.java
    fun Industry.applyDeficitToProductionStatic(index: Int, deficit: Pair<String, Int>, vararg commodities: String) {
        if (this !is BaseIndustry) return
        for (commodity in commodities) {
//			if (this instanceof Mining && market.getName().equals("Louise")) {
//				System.out.println("efwefwe");
//			}
            if (getSupply(commodity).quantity.isUnmodified) continue
            supply(index, commodity, -deficit.two, BaseIndustry.getDeficitText(deficit.one))
        }
    }

}