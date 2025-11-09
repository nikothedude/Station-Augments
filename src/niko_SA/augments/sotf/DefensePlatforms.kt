package niko_SA.augments.sotf

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.BaseAICoreOfficerPluginImpl
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import niko_SA.SA_miscUtils.getFurthestModule
import niko_SA.augments.core.stationAttachment
import org.lazywizard.lazylib.MathUtils

class DefensePlatforms: stationAttachment() {

    companion object {
        const val NUM_TO_DEPLOY = 4
        const val STARTING_ANGLE = 45f
        const val ANGLE_INCR = 360f / NUM_TO_DEPLOY

        val VARIANTS_TO_WEIGHT = mapOf(
            Pair("sotf_empl_t2_lt_Elite", 10f),
            Pair("sotf_empl_t2_lt_Shredder", 10f),
            Pair("sotf_empl_t2_lt_Sniper", 3f)
        )
        fun getPicker(): WeightedRandomPicker<String> {
            val picker = WeightedRandomPicker<String>()
            VARIANTS_TO_WEIGHT.forEach { picker.add(it.key, it.value) }
            return picker
        }
    }

    override fun applyInCombat(station: ShipAPI) {
        val furthestModule = station.getFurthestModule()
        val colRadius = MathUtils.getDistance(station.location, furthestModule.location) + furthestModule.collisionRadius
        val targetRadius = colRadius * 2f

        var numLeft = NUM_TO_DEPLOY
        var currAngle = STARTING_ANGLE
        while (numLeft-- > 0) {
            deployEmplacement(station, targetRadius, currAngle)

            currAngle += ANGLE_INCR
        }
    }

    private fun deployEmplacement(
        station: ShipAPI,
        targetRadius: Float,
        currAngle: Float
    ) {
        val currAngle = Misc.normalizeAngle(currAngle)
        val targetLoc = MathUtils.getPointOnCircumference(station.location, targetRadius, currAngle)
        val variant = getPicker().pick()
        val engine = Global.getCombatEngine()
        val fleetManager = engine.getFleetManager(station.owner)

        val oldSetting = fleetManager.isSuppressDeploymentMessages
        fleetManager.isSuppressDeploymentMessages = true
        val newShip = fleetManager.spawnShipOrWing(
            variant,
            targetLoc,
            MathUtils.getRandomNumberInRange(0f, 360f)
        )
        val core = AICoreOfficerPluginImpl().createPerson(
            Commodities.BETA_CORE,
            Factions.PLAYER,
            MathUtils.getRandom()
        )
        newShip.name = "Weapon Platform"
        newShip.captain = core
        newShip.isAlly = station.isAlly
        newShip.fixedLocation = targetLoc
        fleetManager.isSuppressDeploymentMessages = oldSetting
    }

    override fun getBasicDescription(tooltip: TooltipMakerAPI, expanded: Boolean, panel: CustomPanelAPI?) {
        super.getBasicDescription(tooltip, expanded, panel)

        tooltip.addPara(
            "Deploys a small number of automated weapon platforms around the station, capable of providing heavy - if static - firepower in any engagement.",
            5f
        )

        tooltip.addPara(
            "These platforms are easily replaced, and are %s upon battle end.",
            5f,
            Misc.getPositiveHighlightColor(),
            "immediately replenished"
        )

        tooltip.addPara(
            "This augment's codex entry has a link to the %s.",
            5f,
            Misc.getHighlightColor(),
            "deployed platform"
        ).color = Misc.getGrayColor()
    }
}