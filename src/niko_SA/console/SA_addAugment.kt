package niko_SA.console

import niko_SA.MarketUtils.addStationAugment
import niko_SA.MarketUtils.getStationAugments
import niko_SA.MarketUtils.getStationIndustry
import niko_SA.augments.core.stationAugmentStore
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.CommonStrings
import org.lazywizard.console.Console

class SA_addAugment: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        if (context != BaseCommand.CommandContext.CAMPAIGN_MARKET) {
            Console.showMessage(CommonStrings.ERROR_MARKET_ONLY)
            return BaseCommand.CommandResult.WRONG_CONTEXT
        }
        if (args.isEmpty()) {
            return BaseCommand.CommandResult.BAD_SYNTAX
        }

        val market = context.market
        val station = market?.getStationIndustry()
        if (station == null) {
            Console.showMessage("No station detected!")
            return BaseCommand.CommandResult.ERROR
        }

        val augment = stationAugmentStore.allAugments[args]?.getInstance?.let { it(market) }
        if (augment == null) {
            Console.showMessage("Invalid augment ID!")
            return BaseCommand.CommandResult.ERROR
        }

        if (market.getStationAugments().any { it.id == augment.id }) {
            Console.showMessage("Augment ${augment.name} already present!")
            return BaseCommand.CommandResult.ERROR
        }

        market.addStationAugment(augment)
        Console.showMessage("Augment ${augment.name} successfully applied!")
        return BaseCommand.CommandResult.SUCCESS
    }
}