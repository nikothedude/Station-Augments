package niko_SA.augments.sanctified

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.econ.LuddicMajority
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Factions
import niko_SA.stringUtils.toPercent
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import niko_SA.augments.core.stationAttachment

class sanctifiedStation: stationAttachment() {

    // decreases upkeep, spawns small civilian defense fleets BUT ONLY IF LUDDIC MAJORITY IS ACTIVE
    // these fleets have custom ai and only engage if the station is engaged

    companion object {
        const val UPKEEP_MULT = 0.6f
        const val OUTPUT_INC = 1f
        const val DEMAND_DEC_CREW = 1f
        const val DEMAND_DEC_SUPPLIES = 2f
        const val RESPAWN_DELAY = 16f
        const val MAX_FLEETS = 4
        const val CR_BONUS = 0.15f

        const val FLUX_CAPACITY_DEC = 10f
    }

    var cachedName: String? = null
    var fleetHandler: SanctifiedFleetManager? = null

    override var apToMemberStrengthMult: Float = super.apToMemberStrengthMult
        get() {
            if (isMajorityActive()) return field else return -0.3f // it just sucks
        }

    override fun applyInCombat(station: ShipAPI) {
        for (module in station.childModulesCopy + station) {
            module.mutableStats.fluxCapacity.modifyPercent(id, -FLUX_CAPACITY_DEC)
        }

        return
    }

    override fun apply() {
        super.apply()

        if (isMajorityActive()) {
            val ind = getStationIndustry() ?: return
            ind.upkeep.modifyMult(id, UPKEEP_MULT, getName())

            ind.getDemand(Commodities.CREW).quantity.modifyFlat(id, -DEMAND_DEC_CREW, getName())
            ind.getDemand(Commodities.SUPPLIES).quantity.modifyFlat(id, -DEMAND_DEC_SUPPLIES, getName())

            ind.allSupply.forEach { it.quantity.modifyFlat(id, OUTPUT_INC, getName()) }

            if (fleetHandler == null) {
                fleetHandler = SanctifiedFleetManager(
                    market!!.primaryEntity,
                    50f,
                    getIdealFleets() - 1,
                    getIdealFleets(),
                    RESPAWN_DELAY,
                    this
                )
                Global.getSector().addScript(fleetHandler)
            } else {
                fleetHandler!!.setFleets(getIdealFleets())
                fleetHandler!!.updateFaction(market?.faction)
            }

            val fleet = getStationFleet()
            if (fleet != null) {
                for (member in fleet!!.membersWithFightersCopy) {
                    member.stats.maxCombatReadiness.modifyFlat(id, CR_BONUS, getName())
                }
            }
        }

        val entity = getStationCampaignEntity() ?: return
        if (entity.name != "${market?.name} Station") return // custom names
        cachedName = entity.name
        entity.name = "${market?.name} Sanctum"
    }

    override fun unapply() {
        super.unapply()

        val ind = getStationIndustry() ?: return
        ind.upkeep.unmodify(id)
        ind.allDemand.forEach { it.quantity.unmodify(id) }
        ind.allSupply.forEach { it.quantity.unmodify(id) }
    }

    override fun onRemoved() {
        super.onRemoved()

        if (fleetHandler != null) {
            fleetHandler!!.delete()
            fleetHandler = null
        }
        if (cachedName != null) {
            val entity = getStationCampaignEntity() ?: return
            entity.name = cachedName
        }
    }

    fun isMajority(): Boolean {
        if (market == null) return false
        return (market!!.hasCondition(Conditions.LUDDIC_MAJORITY))
    }

    fun isMajorityActive(): Boolean {
        if (market == null) return false
        return LuddicMajority.matchesBonusConditions(market!!)
    }

    fun getIdealFleets(): Int {
        if (!isMajorityActive() || market?.let { SanctifiedFleetManager.churchExpeditionThreatening(it) } == true) {
            return 0
        } else {
            return (market?.size?.minus(2) ?: 0).coerceAtMost(MAX_FLEETS)
        }
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "\"One of the most clever tricks to increase productivity humanity ever created was religion - by sanctifying a " +
            "place, item, or idea, the faithful would flock to it like flies, maintaining it, building it, defending it, all in the " +
            "name of a god that would never speak to them.\"",
            5f
        ).color = Misc.getGrayColor()
        tooltip.addPara(
            "Overhauls the interior of the orbital station to be a %s - green tapestry adorned over the walls, shrines liberally placed, " +
            "and unnecessary tech stripped out, all in the name of Ludd.",
            5f,
            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
            "Luddic Sanctum"
        )

        tooltip.addSpacer(5f)

        tooltip.addPara(
            "Decreases %s by %s.",
            5f,
            Misc.getNegativeHighlightColor(),
            "flux capacity", "${FLUX_CAPACITY_DEC.toInt()}%"
        )
        if (market == null) {
            tooltip.addPara(
                "If %s is active, has the following effects:",
                5f,
                Misc.getHighlightColor(),
                "luddic majority"
            )
        } else {
            if (isMajorityActive()) {
                tooltip.addPara(
                    "%s is active and %s, resulting in the following benefits:",
                    5f,
                    Misc.getPositiveHighlightColor(),
                    "Luddic majority", "satisfied"
                )
            } else {
                tooltip.addPara(
                    "%s is %s, resulting in the following benefits %s:",
                    5f,
                    Misc.getNegativeHighlightColor(),
                    "Luddic majority", "inactive", "not being applied"
                )
            }
        }
        tooltip.setBulletedListMode(BaseIntelPlugin.BULLET)
        tooltip.addPara(
            "Decreases %s by %s.",
            5f,
            Misc.getPositiveHighlightColor(),
            "upkeep", toPercent(1 - UPKEEP_MULT)
        )
        tooltip.addPara(
            "Decreases %s by %s.",
            5f,
            Misc.getPositiveHighlightColor(),
            "crew and supply demand", "${DEMAND_DEC_CREW.toInt()}/${DEMAND_DEC_SUPPLIES.toInt()}"
        )
        tooltip.addPara(
            "Increases %s by %s.",
            5f,
            Misc.getPositiveHighlightColor(),
            "all supply", "${OUTPUT_INC.toInt()}"
        )
        tooltip.addPara(
            "Increases %s by %s.",
            5f,
            Misc.getPositiveHighlightColor(),
            "station CR", "${(100 * CR_BONUS).toInt()}%"
        )
        tooltip.addPara(
            "Spawns %s that orbit and %s.",
            5f,
            Misc.getPositiveHighlightColor(),
            "vigil fleets", "defend the station"
        ).setHighlightColors(
            Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
            Misc.getPositiveHighlightColor()
        )
        if (market != null && SanctifiedFleetManager.churchExpeditionThreatening(market!!)) {
            tooltip.addPara(
                "Political interference from the %s has %s of the sanctum. Defeat them to regenerate the vigil fleets.",
                5f,
                Misc.getNegativeHighlightColor(),
                "Luddic Expedition", "suppressed would-be luddic protectors"
            ).setHighlightColors(
                Global.getSector().getFaction(Factions.LUDDIC_CHURCH).baseUIColor,
                Misc.getNegativeHighlightColor()
            )
        } else {
            tooltip.setBulletedListMode(BaseIntelPlugin.INDENT + BaseIntelPlugin.BULLET)
            val fleets = getIdealFleets()
            tooltip.addPara(
                "Number of fleets is dependent on market size",
                0f
            )
            if (fleets >= 1) {
                tooltip.addPara(
                    "Currently %s",
                    0f,
                    Misc.getPositiveHighlightColor(),
                    "$fleets"
                )
            }
        }
        tooltip.setBulletedListMode(null)
    }

}