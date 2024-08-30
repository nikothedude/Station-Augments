package niko_SA.augments.core

import com.fs.starfarer.api.campaign.BaseCustomUIPanelPlugin
import com.fs.starfarer.api.campaign.econ.Industry
import com.fs.starfarer.api.campaign.listeners.BaseIndustryOptionProvider
import com.fs.starfarer.api.campaign.listeners.DialogCreatorUI
import com.fs.starfarer.api.campaign.listeners.IndustryOptionProvider.IndustryOptionData
import com.fs.starfarer.api.impl.campaign.econ.impl.OrbitalStation
import indevo.industries.changeling.dialogue.ChangelingIndustryDialogueDelegate

// unused, since you cant open the options panel if youre not in devmode and not in control (FUCK)
class OpenAugmentMenuOptionProvider: BaseIndustryOptionProvider() {
    companion object {
        val CUSTOM_PLUGIN = Any()
    }

    override fun getIndustryOptions(ind: Industry?): MutableList<IndustryOptionData>? {
        if (!isUnsuitable(ind, false)) return null

        val options = ArrayList<IndustryOptionData>()
        options += IndustryOptionData("Open augment menu", CUSTOM_PLUGIN, ind, this)

        return options
    }

    override fun optionSelected(opt: IndustryOptionData?, ui: DialogCreatorUI?) {
        if (opt == null || ui == null) return
        if (opt != CUSTOM_PLUGIN) return

        val delegate = AugmentMenuDialogueDelegate(opt.ind)
        // 2 main modes: see existing hullmods, and modify hullmods
        ui.showDialog(AugmentMenuDialogueDelegate.WIDTH, AugmentMenuDialogueDelegate.HEIGHT, delegate)
    }

    override fun isUnsuitable(ind: Industry?, allowUnderConstruction: Boolean): Boolean {
        return (super.isUnsuitable(ind, allowUnderConstruction) || ind == null || ind !is OrbitalStation)
    }
}