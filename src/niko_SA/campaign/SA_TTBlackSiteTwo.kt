package niko_SA.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.StarSystemAPI
import com.fs.starfarer.api.impl.MusicPlayerPluginImpl
import com.fs.starfarer.api.impl.campaign.WarningBeaconEntityPlugin
import com.fs.starfarer.api.impl.campaign.ids.Commodities
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Industries
import com.fs.starfarer.api.impl.campaign.ids.MemFlags
import com.fs.starfarer.api.impl.campaign.ids.Pings
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.procgen.NebulaEditor
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator
import com.fs.starfarer.api.impl.campaign.procgen.StarSystemGenerator.StarSystemType
import com.fs.starfarer.api.impl.campaign.procgen.themes.BaseThemeGenerator
import com.fs.starfarer.api.impl.campaign.terrain.HyperspaceTerrainPlugin
import com.fs.starfarer.api.impl.campaign.world.TTBlackSite
import com.fs.starfarer.api.util.Misc
import niko_SA.MarketUtils.addStationAugment
import niko_SA.SA_delayedExecution
import niko_SA.SA_ids
import niko_SA.augments.core.BuiltInMode
import niko_SA.augments.core.stationAugmentStore
import org.lazywizard.lazylib.MathUtils
import java.awt.Color

object SA_TTBlackSiteTwo {
    fun generate(): StarSystemAPI {
        if (Global.getSector().memoryWithoutUpdate["\$MPC_SATTBlackSiteTwo"] is StarSystemAPI) return Global.getSector().memoryWithoutUpdate["\$MPC_SATTBlackSiteTwo"] as StarSystemAPI
        val sector = Global.getSector()

        val system: StarSystemAPI = sector.createStarSystem("Unknown Location")
        //system.setType(StarSystemType.NEBULA);
        //system.setType(StarSystemType.NEBULA);
        system.name = "Unknown Location" // to get rid of "Star System" at the end of the name

        system.type = StarSystemType.DEEP_SPACE
        system.addTag(Tags.THEME_UNSAFE)
        system.addTag(Tags.THEME_HIDDEN)
        system.addTag(Tags.THEME_SPECIAL)
        val hyper = Global.getSector().hyperspace

        system.memoryWithoutUpdate[MusicPlayerPluginImpl.MUSIC_SET_MEM_KEY] = "music_campaign_alpha_site"

        system.backgroundTextureFilename = "graphics/backgrounds/background4.jpg"
        //system.getLocation().set(2500, 3000);
        //system.getLocation().set(2500, 3000);
        system.location.set(9000f, 40000f)

        val hyperTerrain = Misc.getHyperspaceTerrain().plugin as HyperspaceTerrainPlugin
        val editor = NebulaEditor(hyperTerrain)
        editor.clearArc(system.location.x, system.location.y, 0f, 500f, 0f, 360f)

        val center = system.initNonStarCenter()
        system.lightColor = Color(225, 170, 255, 255) // light color in entire system, affects all entities
        center.addTag(Tags.AMBIENT_LS)

        val station = BaseThemeGenerator.addSalvageEntity(system, "SA_moteStation", Factions.NEUTRAL)
        station.memoryWithoutUpdate["\$SA_moteStation"] = true
        station.location.set(0f, 0f)
        station.setCircularOrbitWithSpin(center, MathUtils.getRandomNumberInRange(0f, 360f), 1f, 500f, 0.1f, 0.1f)

        SA_delayedExecution(
            @JvmSerializableLambda {
                Misc.setAbandonedStationMarket("SA_moteStationMarket", station)
                val market = station.market
                market.factionId = Factions.MERCENARY
                market.addIndustry(Industries.STARFORTRESS_MID)
                market.getIndustry(Industries.STARFORTRESS_MID)?.finishBuildingOrUpgrading()
                market.getIndustry(Industries.STARFORTRESS_MID)?.isImproved = true
                //market.getIndustry(Industries.STARFORTRESS_HIGH)?.aiCoreId = Commodities.ALPHA_CORE

                market.addStationAugment("SA_moteSink", false)?.builtInMode = BuiltInMode.NORMAL
                market.addStationAugment("SA_bubbleShield", false)?.builtInMode = BuiltInMode.NORMAL
                market.addStationAugment("SA_regenerativeDrones", false)?.builtInMode = BuiltInMode.NORMAL
                market.addStationAugment("SA_ECMPackage", false)?.builtInMode = BuiltInMode.NORMAL
                market.addStationAugment("SA_droneAAF", false)?.builtInMode = BuiltInMode.NORMAL
                market.addStationAugment("SA_fighterTimeflow", false)?.builtInMode = BuiltInMode.NORMAL
                //market.addStationAugment("SA_axialOverclocking", false)?.builtInMode = BuiltInMode.NORMAL

                market.memoryWithoutUpdate?.set(SA_ids.SA_noAugmentAutofit, true)

                market.reapplyIndustries()

                market.factionId = Factions.NEUTRAL

                val fleet = Misc.getStationFleet(market)
                Misc.getStationEntity(market, fleet)?.setFaction(Factions.NEUTRAL)
                fleet?.memoryWithoutUpdate?.set("\$SA_moteFleet", true)
                fleet?.setFaction(Factions.NEUTRAL, true)
                fleet?.commander = fleet?.flagship?.captain
                fleet?.flagship?.repairTracker?.cr = 1f
                //fleet?.memoryWithoutUpdate?.set(MemFlags.MEMORY_KEY_IGNORE_PLAYER_COMMS, true)
                fleet?.memoryWithoutUpdate?.set(MemFlags.MEMORY_KEY_MAKE_HOSTILE, true)
                fleet?.memoryWithoutUpdate?.set(MemFlags.MEMORY_KEY_NO_REP_IMPACT, true)
                fleet?.flagship?.captain = TTBlackSite.createZigguratCaptain()
            },
            0f,
            runWhilePaused = false,
            useDays = false
        ).start()
        val beacon = system.addCustomEntity(null, null, "SA_jumpAnchor", Factions.NEUTRAL)
        beacon.setCircularOrbitWithSpin(station, MathUtils.getRandomNumberInRange(0f, 360f), 1700f, 100f, 2f, 3f)

        system.generateAnchorIfNeeded()

        val well = Global.getSector().createNascentGravityWell(beacon, 30f)
        //well.addTag(Tags.NO_ENTITY_TOOLTIP)
        well.colorOverride = Color(207, 246, 255)
        hyper.addEntity(well)
        well.autoUpdateHyperLocationBasedOnInSystemEntityAtRadius(beacon, 0f)

        beacon.memoryWithoutUpdate["\$SA_TTBlackSiteTwo"] = true
        beacon.memoryWithoutUpdate[WarningBeaconEntityPlugin.PING_ID_KEY] = Pings.WARNING_BEACON1
        beacon.memoryWithoutUpdate[WarningBeaconEntityPlugin.PING_FREQ_KEY] = 0.1f
        beacon.memoryWithoutUpdate[WarningBeaconEntityPlugin.PING_COLOR_KEY] = Color(207, 246, 255, 255)
        beacon.memoryWithoutUpdate[WarningBeaconEntityPlugin.GLOW_COLOR_KEY] = Color(255, 255, 255, 255)

        Global.getSector().memoryWithoutUpdate["\$MPC_SATTBlackSiteTwo"] = system
        return system
    }
}