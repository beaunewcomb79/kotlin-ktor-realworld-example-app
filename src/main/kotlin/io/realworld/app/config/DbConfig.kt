package io.realworld.app.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.h2.tools.Server
import org.jetbrains.exposed.sql.Database

object DbConfig {
    // The PG-compatibility server binds a fixed TCP port, so starting it more than once
    // (the integration tests call setup() before every test method) throws "port in use".
    // Start it at most once per JVM.
    private var pgServerStarted = false

    fun setup(jdbcUrl: String, username: String, password: String) {
        if (!pgServerStarted) {
            Server.createPgServer().start()
            pgServerStarted = true
        }
        val config = HikariConfig().also { config ->
            config.jdbcUrl = jdbcUrl
            config.username = username
            config.password = password
        }
        Database.connect(HikariDataSource(config))
    }
}
