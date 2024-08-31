package niko_SA

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

object MarketUtils {
    /** Returns the augment budget this station has. An augment budget controls how many augments a station can have - each augment has its own cost.*/
    fun OrbitalStation.getAugmentBudget(): Float {
        var points = stationAttachment.BASE_STATION_AUGMENT_BUDGET

        for (tag in spec.tags) {
            val tagBonus = stationAttachment.tagToExtraAugmentBudget[tag]
            if (tagBonus != null) {
                points += tagBonus
            }
        }
        return points
    }

    fun MarketAPI.getUsedAugmentBudget(): Float {
        var used = 0f

        for (augment in getStationAugments()) {
            used += augment.augmentCost
        }

        return used
    }

    fun OrbitalStation.getUsedAugmentBudget(): Float {
        return market.getUsedAugmentBudget()
    }

    fun OrbitalStation.getRemainingAugmentBudget(): Float {
        val budget = getAugmentBudget()
        val usedBudget = getUsedAugmentBudget()

        return (budget - usedBudget)
    }

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

    fun MarketAPI.toggleStationAugment(instance: stationAttachment, checkForStation: Boolean = true) {
        if (instance.applied) {
            instance.unapply()
            getStationAugments() -= instance
        } else {
            addStationAugment(instance, checkForStation)
        }
    }

    fun MarketAPI.addStationAugment(augment: stationAttachment, checkForStation: Boolean = true) {
        if (hasStationAugment(augment)) {
            SA_debugUtils.log.error("tried to add ${augment.id} while $name already had it!")
            return
        }
        if (checkForStation) {
            val industry = getStationIndustry()
            if (industry == null) {
                SA_debugUtils.log.info("$name has no station, aborting addition of ${augment.id}")
                return
            }
        }
        augment.apply()
        getStationAugments() += augment
    }

    fun MarketAPI.removeStationAugment(augment: stationAttachment) {
        augment.unapply()
        getStationAugments() -= augment
    }

    fun MarketAPI.getStationIndustry(): Industry? {
        return Misc.getStationIndustry(this)
    }

    fun MarketAPI.hasStationAugment(augment: stationAttachment): Boolean {
        return hasStationAugment(augment.id)
    }

    fun MarketAPI.hasStationAugment(augmentId: String): Boolean {
        return getStationAugments().any { it.id == augmentId }
    }

}