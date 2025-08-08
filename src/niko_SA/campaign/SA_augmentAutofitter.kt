package niko_SA.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.util.Misc
import com.fs.starfarer.api.util.WeightedRandomPicker
import niko.MCTE.settings.MCTE_settings
import niko_SA.MarketUtils.addStationAugment
import niko_SA.MarketUtils.getRemainingAugmentBudget
import niko_SA.MarketUtils.getStationAugments
import niko_SA.MarketUtils.getStationIndustry
import niko_SA.SA_delayedExecution
import niko_SA.SA_ids
import niko_SA.SA_ids.SA_lastAPValueMemid
import niko_SA.SA_mathUtils.prob
import niko_SA.SA_settings
import niko_SA.augments.core.stationAugmentSpec
import niko_SA.augments.core.stationAugmentStore
import niko_SA.augments.core.stationAugmentStore.getKnownAugments
import org.lazywizard.lazylib.MathUtils
import kotlin.math.log

class SA_augmentAutofitter: BaseCampaignEventListener(false) {

    override fun reportPlayerOpenedMarket(market: MarketAPI?) {
        super.reportPlayerOpenedMarket(market)

        if (market == null) return
        tryAutofit(market)
    }

    override fun reportFleetJumped(
        fleet: CampaignFleetAPI?,
        from: SectorEntityToken?,
        to: JumpPointAPI.JumpDestination?
    ) {
        super.reportFleetJumped(fleet, from, to)

        if (fleet == null || fleet != Global.getSector().playerFleet) return
        if (to == null) return
        for (market in Global.getSector().economy.getMarkets(to.destination.containingLocation)) {
            tryAutofit(market)
        }
    }

    override fun reportEconomyMonthEnd() {
        super.reportEconomyMonthEnd()

        SA_delayedExecution(
            @JvmSerializableLambda {
                for (market in Global.getSector().economy.marketsCopy) {
                    tryAutofit(market)
                }
            },
            MathUtils.getRandomNumberInRange(0.1f, 0.6f),
            false,
            useDays = true
        ).start() // delay it to avoid causing a lagspike
    }

    companion object {

        fun tryAutofit(market: MarketAPI) {
            if (market.canGetAugmentAutofit() && market.needsAugmentAutofit()) {
                autofitMarket(market)
            }
        }

        fun MarketAPI.needsAugmentAutofit(): Boolean {
            if (!SA_settings.AUTOFIT_ENABLED) return false
            val budget = getRemainingAugmentBudget()
            if (budget <= 0) return false
            if (getStationAugments().isEmpty()) return true
            val lastAP = memoryWithoutUpdate.getFloat(SA_lastAPValueMemid)

            return budget != lastAP
        }

        fun MarketAPI.canGetAugmentAutofit(): Boolean {
            if (isPlayerOwned) {
                memoryWithoutUpdate[SA_lastAPValueMemid] = getRemainingAugmentBudget() // just to cache for later
                return false
            }
            if (getStationIndustry() == null) return false
            if (memoryWithoutUpdate.getBoolean(SA_ids.SA_noAugmentAutofit) || (faction.custom.optBoolean("SA_noFactionSAAutofit", false))) return false
            return true
        }

        fun autofitMarket(market: MarketAPI) {
            var combatWeight = 9f
            var logisticWeight = 4f
            if (Misc.isMilitary(market)) {
                logisticWeight *= 0.1f
            } else {
                combatWeight *= 0.3f
            }

            val picker = WeightedRandomPicker<stationAugmentSpec>()
            for (id in market.faction.getKnownAugments()) {
                var weight = 0f
                val spec = stationAugmentStore.allAugments[id] ?: continue
                for (usageTag in spec.usageTags) {
                    val regex = Regex("(\\d+|\\D+)")
                    val brokenUp = regex.findAll(usageTag).map { it.groupValues.first() }.toList()

                    val base = brokenUp[0]
                    val rating = if (brokenUp.size > 1) brokenUp[1].toFloat() else 0f

                    when (base) {
                        "combatgood" -> {
                            weight += rating * combatWeight
                        }
                        "combatBad" -> {
                            weight -= rating * combatWeight
                        }
                        "logisticsWeight" -> {
                            weight += rating * logisticWeight
                        }
                        "skipautofitchance" -> {
                            if (prob(rating)) {
                                weight = -1f
                                break
                            }
                        }
                    }
                }
                if (spec.autofitPlugin != null) {
                    weight = spec.autofitPlugin!!.modifyAutofitWeight(weight, spec, market)
                }
                if (weight >= 0f) {
                    picker.add(spec, weight)
                }
            }
            val increasePicker = WeightedRandomPicker<stationAugmentSpec>()
            for (entry in picker.items.filter { it.apCost <= 0f }) { // we sort so these go first
                increasePicker.add(entry, picker.getWeight(entry))
                picker.remove(entry)
            }
            while (!increasePicker.isEmpty || !picker.isEmpty) {
                val picked = increasePicker.pickAndRemove() ?: picker.pickAndRemove()

                val instance = picked.getNewPluginInstance(market)
                instance.considerEngagement = false
                instance.considerReqItem = false
                if (!instance.canBeModifiedOrBuilt()) continue
                instance.considerEngagement = true
                instance.considerReqItem = true

                market.addStationAugment(instance)
            }
            market.memoryWithoutUpdate[SA_lastAPValueMemid] = market.getRemainingAugmentBudget()
        }
    }
}