package niko_SA.campaign

object SA_specialProcgenHandler {
    fun doSpecialProcgen(checkExisting: Boolean = false) {
        generateExplorationContent()

    }

    private fun generateExplorationContent() {
        generateMoteStation()
    }

    private fun generateMoteStation() {
        SA_TTBlackSiteTwo.generate()
    }
}