package niko_SA.console

import niko_SA.campaign.SA_specialProcgenHandler.doSpecialProcgen
import org.lazywizard.console.BaseCommand
import org.lazywizard.console.Console

class SA_genSpecialContent: BaseCommand {
    override fun runCommand(args: String, context: BaseCommand.CommandContext): BaseCommand.CommandResult {
        doSpecialProcgen(true)

        Console.showMessage("special content updated to current version! note this does not guarantee perfect parity.")
        return BaseCommand.CommandResult.SUCCESS
    }
}