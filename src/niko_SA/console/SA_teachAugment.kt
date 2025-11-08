package niko_SA.console

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import niko_SA.MarketUtils.getStationAugments
import niko_SA.augments.core.stationAugmentStore
import niko_SA.augments.core.stationAugmentStore.allAugments
import niko_SA.augments.core.stationAugmentStore.getKnownAugments
import niko_SA.augments.core.stationAugmentStore.teachAugment
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.BaseCommandWithSuggestion
import org.lazywizard.console.CommonStrings
import org.lazywizard.console.Console

class SA_teachAugment: BaseCommandWithSuggestion {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        if (Global.getCurrentState() != GameState.CAMPAIGN) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY)
            return BaseCommand.CommandResult.WRONG_CONTEXT
        }

        val tmp = args.split(" ".toRegex(), limit = 2)
        if (tmp.size < 2) {
            return BaseCommand.CommandResult.BAD_SYNTAX
        }

        val factionId = tmp[0]
        val code = tmp[1]

        val faction = Global.getSector().getFaction(factionId)
        if (faction == null) {
            Console.showMessage("Invalid faction ID!")
            return BaseCommand.CommandResult.ERROR
        }

        if (code == "all") {
            for (augment in allAugments) {
                faction.teachAugment(augment.key)
            }
            Console.showMessage("Taught faction ${faction.displayName} all station augments.")
            return BaseCommand.CommandResult.SUCCESS
        }
        val augment = allAugments[code]
        if (augment == null) {
            Console.showMessage("Invalid augment ID!")
            return BaseCommand.CommandResult.ERROR
        }
        faction.teachAugment(code)
        Console.showMessage("Taught faction ${faction.displayName} augment $code.")
        return BaseCommand.CommandResult.SUCCESS
    }

    override fun getSuggestions(
        parameter: Int,
        previous: List<String?>?,
        context: BaseCommand.CommandContext?
    ): List<String>? {
        when (parameter) {
            0 -> {
                return Global.getSector().allFactions.map { it.id }
            }
            1 -> {
                return listOf("all") + allAugments.keys.toList()
            }
        }

        return null
    }
}