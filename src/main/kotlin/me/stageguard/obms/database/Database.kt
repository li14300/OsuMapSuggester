package me.stageguard.obms.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.stageguard.obms.PluginConfig
import me.stageguard.obms.OsuMapSuggester
import me.stageguard.obms.database.model.BeatmapSkillTable
import me.stageguard.obms.database.model.OsuUserInfo
import net.mamoe.mirai.utils.error
import net.mamoe.mirai.utils.info
import net.mamoe.mirai.utils.warning
import okhttp3.internal.closeQuietly
import org.ktorm.database.Database
import org.ktorm.database.Transaction
import java.lang.IllegalArgumentException

object Database {

    sealed class ConnectionStatus {
        object CONNECTED : ConnectionStatus()
        object DISCONNECTED : ConnectionStatus()
    }

    private lateinit var db : Database
    private lateinit var hikariSource : HikariDataSource
    private var connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED

    suspend fun <T> query(block: suspend Transaction.(Database) -> T) : T? = if(connectionStatus == ConnectionStatus.DISCONNECTED) {
        OsuMapSuggester.logger.error { "Database is disconnected and the query operation will not be completed." }
        null
    } else db.useTransaction { block(it, db) }

    fun connect() {
        db = Database.connect(hikariDataSourceProvider().also { hikariSource = it })
        connectionStatus = ConnectionStatus.CONNECTED
        initDatabase()
        OsuMapSuggester.logger.info { "Database ${PluginConfig.database.table} is connected." }
    }

    fun isConnected() = connectionStatus == ConnectionStatus.CONNECTED

    private fun initDatabase() {
        // ktorm doesn't support creating database schema.
        db.useConnection { connection ->
            val statement = connection.createStatement()
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `${OsuUserInfo.tableName}` (
                  `id` int NOT NULL AUTO_INCREMENT,
                  `osuId` int NOT NULL,
                  `osuName` varchar(16) NOT NULL,
                  `qq` bigint NOT NULL,
                  `token` varchar(1500) NOT NULL,
                  `tokenExpiresUnixSecond` bigint NOT NULL,
                  `refreshToken` varchar(1500) NOT NULL,
                  PRIMARY KEY (`id`),
                  UNIQUE KEY `users_qq_unique` (`qq`)
                );
            """.trimIndent())
            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS `${BeatmapSkillTable.tableName}` (
                	`id` INT NOT NULL AUTO_INCREMENT,
                	`bid` INT NOT NULL,
                	`stars` DOUBLE NOT NULL,
                	`jump` DOUBLE NOT NULL,
                	`flow` DOUBLE NOT NULL,
                	`speed` DOUBLE NOT NULL,
                	`stamina` DOUBLE NOT NULL,
                	`precision` DOUBLE NOT NULL,
                	`complexity` DOUBLE NOT NULL,
                	PRIMARY KEY (`id`),
                    UNIQUE KEY `beatmap_skills_unique` (`bid`)
                );
            """.trimIndent())
        }
    }

    fun close() {
        connectionStatus = ConnectionStatus.DISCONNECTED
        hikariSource.closeQuietly()
    }

    private fun hikariDataSourceProvider() : HikariDataSource = HikariDataSource(HikariConfig().apply {
        when {
            PluginConfig.database.address == "" -> throw IllegalArgumentException("Database address is not set in config file ${PluginConfig.saveName}.")
            PluginConfig.database.table == "" -> {
                OsuMapSuggester.logger.warning { "Database table is not set in config file ${PluginConfig.saveName} and now it will be default value 'sctimetabledb'." }
                PluginConfig.database.table = "osu!beatmap suggester"
            }
            PluginConfig.database.port !in 1024..65535 -> throw IllegalArgumentException("Database port is invalid.")
            PluginConfig.database.user == "" -> throw IllegalArgumentException("Database user is not set in config file ${PluginConfig.saveName}.")
            PluginConfig.database.password == "" -> throw IllegalArgumentException("Database password is not set in config file ${PluginConfig.saveName}.")
            PluginConfig.database.maximumPoolSize == null -> {
                OsuMapSuggester.logger.warning { "Database maximumPoolSize is not set in config file ${PluginConfig.saveName} and now it will be default value 10." }
                PluginConfig.database.maximumPoolSize = 10
            }
        }
        jdbcUrl         = "jdbc:mysql://${PluginConfig.database.address}:${PluginConfig.database.port}/${PluginConfig.database.table}"
        driverClassName = "com.mysql.cj.jdbc.Driver"
        username        = PluginConfig.database.user
        password        = PluginConfig.database.password
        maximumPoolSize = PluginConfig.database.maximumPoolSize!!
    })

}