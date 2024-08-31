package niko_SA

import com.fs.starfarer.api.Global
import data.utilities.niko_MPC_debugUtils
import org.apache.log4j.Level
import org.apache.log4j.Logger

object SA_debugUtils {
    val log: Logger = Global.getLogger(SA_debugUtils::class.java)

    init {
        niko_MPC_debugUtils.log.level = Level.ALL
    }
}