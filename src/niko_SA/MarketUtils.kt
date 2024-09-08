package niko_SA

import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.util.Misc
import niko_SA.SA_settings.BASE_STATION_AUGMENT_BUDGET
import niko_SA.augments.core.stationAttachment
import niko_SA.augments.core.stationAttachment.Companion.stationImprovedAPBonus

object MarketUtils {
    /** Returns the augment budget this station has. An augment budget controls how many augments a station can have - each augment has its own cost.*/
    @JvmStatic
    fun OrbitalStation.getAugmentBudget(): Float {
        var points = BASE_STATION_AUGMENT_BUDGET
        if (isImproved) points += stationImprovedAPBonus

        for (tag in spec.tags) {
            val tagBonus = stationAttachment.tagToExtraAugmentBudget[tag]
            if (tagBonus != null) {
                points += tagBonus
            }
        }
        return points
    }

    @JvmStatic
    fun MarketAPI.getUsedAugmentBudget(): Float {
        var used = 0f

        for (augment in getStationAugments()) {
            used += augment.augmentCost
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
            instance.unapply()
            getStationAugments() -= instance
        } else {
            addStationAugment(instance, checkForStation)
        }
    }

    @JvmStatic
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

    @JvmStatic
    fun MarketAPI.removeStationAugment(augment: stationAttachment) {
        augment.unapply()
        getStationAugments() -= augment
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

}