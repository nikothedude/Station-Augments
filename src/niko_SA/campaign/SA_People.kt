package niko_SA.campaign

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.Ranks
import data.utilities.niko_MPC_ids

object SA_People {

    const val DIKTAT_AGENT = "SA_diktatMoteAgent"
    const val REACTIONARY_PATHER = "SA_reactionaryPather"

    fun getImportantPeople(): HashMap<String, PersonAPI> {
        if (Global.getSector().memoryWithoutUpdate["\$SA_importantPeople"] == null) {
            Global.getSector().memoryWithoutUpdate["\$SA_importantPeople"] = HashMap<String, PersonAPI>()
        }
        return Global.getSector().memoryWithoutUpdate["\$SA_importantPeople"] as HashMap<String, PersonAPI>
    }

    fun createCharacters() {
        val importantPeople = Global.getSector().importantPeople

        val SA_importantPeople = getImportantPeople()

        if (SA_importantPeople[DIKTAT_AGENT] == null) {
            val agent = Global.getSector().getFaction(Factions.DIKTAT).createRandomPerson(FullName.Gender.MALE)

            agent.id = DIKTAT_AGENT
            agent.rankId = Ranks.SPECIAL_AGENT
            agent.postId = Ranks.POST_SPECIAL_AGENT

            SA_importantPeople[DIKTAT_AGENT] = agent
            importantPeople.addPerson(agent)
        }
        if (SA_importantPeople[REACTIONARY_PATHER] == null) {
            val agent = Global.getSector().getFaction(Factions.LUDDIC_PATH).createRandomPerson(FullName.Gender.MALE)

            agent.id = REACTIONARY_PATHER
            agent.rankId = Ranks.CITIZEN
            agent.postId = Ranks.POST_CITIZEN

            SA_importantPeople[REACTIONARY_PATHER] = agent
            importantPeople.addPerson(agent)
        }
    }

}