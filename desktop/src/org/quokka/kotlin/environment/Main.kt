package org.quokka.kotlin.environment

import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import org.quokka.game.desktop.GameInitializer
import kotlinx.cli.*
import org.quokka.kotlin.internals.DbConnectionPool

fun main(args: Array<String>) {
    val parser = ArgParser("quokka")
    val address by parser.option(ArgType.String, shortName = "a").default("localhost")
    val name by parser.option(ArgType.String, shortName = "n").default("lidar")
    val username by parser.option(ArgType.String, shortName = "u").default("lidar")
    val password by parser.option(ArgType.String, shortName = "p").default("mindhash")
    parser.parse(args)
    DbConnectionPool.setup("jdbc:postgresql://${address}/${name}", username, password)
    val config = LwjglApplicationConfiguration()
    config.height = 720
    config.width = 1280
    config.forceExit = true
    LwjglApplication(GameInitializer, config)
}
