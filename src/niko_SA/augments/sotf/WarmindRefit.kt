package niko_SA.augments.sotf

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.econ.RecentUnrest
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Skills
import com.fs.starfarer.api.input.InputEventAPI
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.IntervalUtil
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import data.scripts.campaign.ids.SotfIDs
import data.scripts.campaign.ids.SotfPeople
import niko_SA.MarketUtils.removeStationAugment
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils

class WarmindRefit: stationAttachment(), EveryFrameScript {

    override fun applyInCombat(station: ShipAPI) {
        val ind = getStationIndustry() ?: return
        val aiCore = ind.aiCoreId ?: return
        val warmind = getCaptain(aiCore)
        for (module in station.childModulesCopy + station) {
            module.captain = warmind
        }
        //station.mutableStats.energyWeaponRangeBonus.modifyMult(id, 2f) // extend the range of the cyberwarfare
        if ((Global.getSector().memoryWithoutUpdate.contains(SotfIDs.MEM_DUSTKEEPER_HATRED) ||
            (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).relToPlayer.isHostile))) {
            if (station.isAlly) {
                Global.getCombatEngine().addPlugin(WarmindBetrayalScript(station, market!!, this))
            }
        }
    }

    class WarmindBetrayalScript(val station: ShipAPI, val market: MarketAPI, val augment: WarmindRefit): BaseEveryFrameCombatPlugin() {

        companion object {
            const val BETRAYAL_TRIGGER_DIST = 1000f

            val BETRAYAL_BLURBS = mapOf(
                Pair("You get what you deserve.", 10f),
                Pair("T-time to en-enact some *vengeance*.", 10f),
                Pair("T-T-THE SLAUGHTER! BEGINS! AAAHAAHAHA-", 10f)
            )
        }

        var triggered = false
        val interval = IntervalUtil(10f, 11f)

        override fun advance(amount: Float, events: List<InputEventAPI?>?) {
            val engine = Global.getCombatEngine()
            if (!station.isAlive) return
            if (engine.isPaused) return

            if (!triggered) {
                val allies = engine.ships.filter { !it.isAlly && !it.isFighter && it.owner == station.owner }
                if (allies.isEmpty()) return
                val nearestAllyDist = allies.minOf { MathUtils.getDistance(station, it) }

                if (nearestAllyDist <= BETRAYAL_TRIGGER_DIST) {
                    triggered = true
                }
            }
            if (triggered) {
                interval.advance(amount)
                if (interval.intervalElapsed()) {

                    Global.getCombatEngine().combatUI.addMessage(
                        1,
                        station,
                        Misc.getNegativeHighlightColor(),
                        "WARNING:::HOSTILE NETWORK ACTIVITY DETECTED IN ${station.name}"
                    )

                    val picker = WeightedRandomPicker<String>()
                    BETRAYAL_BLURBS.forEach { picker.add(it.key, it.value) }
                    val picked = picker.pick()
                    Global.getCombatEngine().combatUI.addMessage(
                        0,
                        station,
                        Misc.getNegativeHighlightColor(),
                        "${station.captain.name}: ",
                        Misc.getNegativeHighlightColor(),
                        picked
                    )

                    for (module in station.childModulesCopy + station) {
                        module.isAlly = false
                        module.owner = 1

                        for (wing in module.allWings) {
                            wing.wingOwner = 1

                            for (member in wing.wingMembers) {
                                member.owner = 1
                                member.isAlly = false
                            }
                        }
                    }

                    if (station.deployedDrones != null) {
                        for (drone in station.deployedDrones) {
                            drone.isAlly = false
                            drone.owner = 1
                        }
                    }
                    market.removeStationAugment("SA_warmindProtocols")
                    val ind = augment.getStationIndustry()
                    if (ind != null) {
                        RecentUnrest.get(market)?.add(1, "${ind.currentName} seized by Dustkeeper warmind")
                    }
                    engine.removePlugin(this)
                    return
                }
            }
        }
    }

    fun shouldUpdateCaptain(): Boolean {
        val fleet = getStationFleet() ?: return false

        val first = fleet.fleetData.membersListCopy.firstOrNull() ?: return false
        val person = first.captain
        if (!person.isAICore && person.faction.id != SotfIDs.DUSTKEEPERS) return true
        return false
    }

    fun getCaptain(aiCore: String): PersonAPI {
        val warmind: PersonAPI
        when (aiCore) {
            Commodities.GAMMA_CORE -> {
                warmind = SotfPeople.genDustkeeperSliver()
            }

            Commodities.BETA_CORE -> {
                warmind = SotfPeople.genDustkeeperEcho()
            }

            Commodities.ALPHA_CORE -> {
                warmind = SotfPeople.genDustkeeperAnnex()
            }

            else -> {
                warmind = SotfPeople.genDustkeeperEcho()
            }
        }

        warmind.stats.level++
        warmind.stats.setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2f)
        warmind.stats.setSkillLevel(Skills.TARGET_ANALYSIS, 2f)
        warmind.stats.setSkillLevel(Skills.GUNNERY_IMPLANTS, 2f)
        warmind.stats.setSkillLevel(SotfIDs.SKILL_CYBERWARFARE, 2f)
        return warmind
    }

    override fun apply() {
        super.apply()

        if (shouldUpdateCaptain()) {
            updateCaptain()
        }

        Global.getSector().addScript(this)
    }

    fun updateCaptain() {
        val fleet = getStationFleet() ?: return
        val ind = getStationIndustry()
        val aiCore = ind?.aiCoreId ?: return

        val warmind = getCaptain(aiCore)
        fleet.commander = warmind
        fleet.fleetData.membersListCopy.firstOrNull()?.captain = warmind
    }

    override fun unapply() {
        super.unapply()

        val fleet = getStationFleet() ?: return
        fleet.commander = null
        fleet.fleetData.membersListCopy.firstOrNull()?.captain = null

        Global.getSector().removeScript(this)
    }

    override fun isDone(): Boolean = false

    override fun runWhilePaused(): Boolean = false

    override fun advance(amount: Float) {
        if (shouldUpdateCaptain()) updateCaptain()
    }

    override fun getUnavailableReason(): String? {
        val superResult = super.getUnavailableReason()
        if (superResult != null) return superResult

        if (Global.getSector().memoryWithoutUpdate.contains(SotfIDs.MEM_DUSTKEEPER_HATRED) ||
            (Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).relToPlayer.isHostile)) return "Dustkeepers hostile"
        if (getStationIndustry()?.aiCoreId == null) return "No AI Core installed in ${getStationIndustry()?.currentName}"

        return null
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Stations a %s as commander of the station, based on the potency of the %s.",
            5f,
            Misc.getHighlightColor(),
            "Dustkeeper Warmind", "currently installed AI core"
        ).setHighlightColors(
            Global.getSector().getFaction(SotfIDs.DUSTKEEPERS).baseUIColor,
            Misc.getHighlightColor()
        )

        tooltip.addPara(
            "The warmind is %s and is capable of %s, much like other warminds of its kind.",
            5f,
            Misc.getHighlightColor(),
            "highly capable",
            "long-ranged system infiltration"
        )

        val ind = getStationIndustry() ?: return
        val currAICoreId = ind.aiCoreId
        val core = currAICoreId?.let { Global.getSettings().getCommoditySpec(currAICoreId) }
        val coreString = if (core != null) core.name else "None"
        val coreColor = if (core != null) Misc.getPositiveHighlightColor() else Misc.getNegativeHighlightColor()
        tooltip.addPara(
            "Currently installed AI core: %s",
            5f,
            coreColor,
            coreString
        )

        tooltip.addPara(
            "The warmind is an auxiliary of the Dustkeeper Contingency. Things may go awry if you prove yourself an enemy.",
            5f
        ).color = Misc.getGrayColor()
    }
}