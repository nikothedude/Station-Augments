package niko_SA.console

import niko_SA.MarketUtils.addStationAugment
import niko_SA.MarketUtils.getStationAugments
import niko_SA.MarketUtils.getStationIndustry
import niko_SA.MarketUtils.removeStationAugment
import niko_SA.augments.core.stationAugmentStore
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.CommonStrings
import org.lazywizard.console.Console

class SA_removeAugment: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        if (context != BaseCommand.CommandContext.CAMPAIGN_MARKET) {
            Console.showMessage(CommonStrings.ERROR_MARKET_ONLY)
            return BaseCommand.CommandResult.WRONG_CONTEXT
        }
        if (args.isEmpty()) {
            return BaseCommand.CommandResult.BAD_SYNTAX
        }
        val market = context.market ?: return BaseCommand.CommandResult.ERROR
        /*val station = market?.getStationIndustry()
        if (station == null) {
            Console.showMessage("No station detected!")
            return BaseCommand.CommandResult.ERROR
        }

        val augment = stationAugmentStore.allAugments[args]?.getInstance?.let { it(market) }
        if (augment == null) {
            Console.showMessage("Invalid augment ID!")
            return BaseCommand.CommandResult.ERROR
        }*/

        val augment = market.getStationAugments().firstOrNull() { it.id == args }
        if (augment == null) {
            Console.showMessage("The requested augment isnt present on the market.")
            return BaseCommand.CommandResult.SUCCESS
        }

        market.removeStationAugment(augment)
        Console.showMessage("Augment ${augment.name} successfully removed!")
        return BaseCommand.CommandResult.SUCCESS
    }
}