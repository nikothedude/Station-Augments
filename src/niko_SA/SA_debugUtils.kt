package niko_SA

import com.fs.starfarer.api.Global
import org.apache.log4j.Level
import org.apache.log4j.Logger

object SA_debugUtils {
    val log: Logger = Global.getLogger(SA_debugUtils::class.java)

    init {
        log.level = Level.ALL
    }
}