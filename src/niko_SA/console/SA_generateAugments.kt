package niko_SA.console

import com.fs.starfarer.api.GameState
import com.fs.starfarer.api.Global
import niko_SA.SA_settings
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.CommonStrings
import org.lazywizard.console.Console

class SA_generateAugments: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        if (Global.getCurrentState() != GameState.CAMPAIGN) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY)
            return BaseCommand.CommandResult.WRONG_CONTEXT
        }

        SA_settings.applyPredefinedAugments()

        Console.showMessage("Success! Augments have been applied to all pre-defined markets.")
        return BaseCommand.CommandResult.SUCCESS
    }
}