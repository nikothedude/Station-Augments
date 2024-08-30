package niko_SA.genericIndustries

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.CampaignFleetAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.econ.impl.BaseIndustry
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import com.fs.starfarer.api.impl.campaign.ids.HullMods
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.ReflectionUtils
import niko_SA.SA_ids
import niko_SA.genericIndustries.stationAttachment.Companion.getRemainingAugmentBudget

/** Industries of this type attempt to modify an existing station in combat, and potentially, in campaign.*/
abstract class stationAttachment: BaseIndustry() {

    /** The "cost" to be subtracted from our stations augment budget. We cannot be built if our station doesnt have enough budget for us. */
    abstract val augmentCost: Float
    var reapplying = false

    /** We can only be built on stations with these industry ids. If empty, is ignored. */
    open val stationTypeWhitelist = HashSet<String>()

    companion object {
        const val BASE_STATION_AUGMENT_BUDGET = 20f // arbitrary
        /** Additive atop BASE_STATION_AUGMENT_BUDGET. */
        val tagToExtraAugmentBudget = hashMapOf(
            Pair(Industries.BATTLESTATION, 10f),
            Pair(Industries.STARFORTRESS, 20f)
        )

        /** Returns the augment budget this station has. An augment budget controls how many augments a station can have - each augment has its own cost.*/
        fun OrbitalStation.getAugmentBudget(): Float {
            var points = BASE_STATION_AUGMENT_BUDGET

            for (tag in spec.tags) {
                val tagBonus = tagToExtraAugmentBudget[tag]
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
            val augments = HashSet<stationAttachment>()

            for (industry in industries) {
                if (!industry.isFunctional) continue
                if (industry.spec.hasTag(SA_ids.SA_structureTag)) {
                    augments += (industry as stationAttachment)
                }
            }

            return augments
        }
    }

    /** Ran once at the beginning of combat. */
    abstract fun applyInCombat(station: ShipAPI)
    /*override fun apply() {
        val stationFleet = getStationFleet()
        stationFleet?.fleetData?.membersListCopy?.firstOrNull()?.variant?.addMod(HullMods.HEAVYARMOR)
    }*/

    override fun reapply() {
        reapplying = true
        super.reapply()
        reapplying = false
    }

    override fun isAvailableToBuild(): Boolean {
        val stationIndustry = getStationIndustry() ?: return false
        if (stationIndustry.getRemainingAugmentBudget() < augmentCost) return false
        if (stationTypeWhitelist.isNotEmpty() && !stationTypeWhitelist.contains(stationIndustry.spec.id)) return false

        return true
    }

    override fun getUnavailableReason(): String {
        val station = getStationIndustry() ?: return "No orbital station"
        if (stationTypeWhitelist.isNotEmpty() && !stationTypeWhitelist.contains(station.spec.id)) {
            return "Incorrect station type - requires ${getNeededStationTypeText()}"
        }
        if (station.getRemainingAugmentBudget() < augmentCost) return "Not enough augment points to install"
        return super.getUnavailableReason()
    }

    /** Only needed if [stationTypeWhitelist] is not empty. */
    open fun getNeededStationTypeText(): String {
        return "error! please report to the mod author"
    }

    /** Returns the orbital station industry instance. Required to not be null for us to be buildable.*/
    fun getStationIndustry(): OrbitalStation? {
        for (industry in getMarket().industries) {
            if (industry.spec.hasTag(Tags.STATION)) {
                return industry as OrbitalStation
            }
        }
        return null
    }

    fun getStationFleet(): CampaignFleetAPI? {
        val stationIndustry = getStationIndustry() ?: return null
        return ReflectionUtils.get("stationFleet", stationIndustry) as? CampaignFleetAPI
    }

    /** Returns the in-combat station entity we are affecting. Returns null if we're not in combat, or it doesnt exist. */
    fun getStationCombatEntity(): ShipAPI? {
        if (Global.getCurrentState() != GameState.COMBAT) return null
        val engine = Global.getCombatEngine()

        for (ship in engine.ships) {
            if (ship.isStation) {
                val member = ship.fleetMember ?: continue
                val fleet = member.fleetData?.fleet ?: continue
                if (fleet.memoryWithoutUpdate[MemFlags.STATION_MARKET] == getMarket()) {
                    return ship
                }
            }
        }
        return null
    }

    fun getStationCampaignEntity(): SectorEntityToken? {
        val stationIndustry = getStationIndustry() ?: return null
        return ReflectionUtils.get("stationEntity", stationIndustry) as? SectorEntityToken
    }

    override fun createTooltip(mode: Industry.IndustryTooltipMode?, tooltip: TooltipMakerAPI?, expanded: Boolean) {
        super.createTooltip(mode, tooltip, expanded)

        if (tooltip == null) return
        val orbitalStation = getStationIndustry() ?: return
        val remainingAugmentBudget = orbitalStation.getRemainingAugmentBudget()
        val para = tooltip.addPara(
            "This augment costs %s AP to install. The ${orbitalStation.currentName} currently has %s AP remaining.",
            5f,
            Misc.getHighlightColor(),
            "$augmentCost", "$remainingAugmentBudget"
        )
        val augmentBudgetColor = if (remainingAugmentBudget < augmentCost) Misc.getNegativeHighlightColor() else Misc.getHighlightColor()
        para.setHighlightColors(Misc.getHighlightColor(), augmentBudgetColor)
    }
}