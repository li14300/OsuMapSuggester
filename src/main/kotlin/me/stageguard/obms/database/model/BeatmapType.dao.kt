package me.stageguard.obms.database.model

import me.stageguard.obms.database.AddableTable
import org.ktorm.dsl.AssignmentsBuilder
import org.ktorm.entity.Entity
import org.ktorm.schema.date
import org.ktorm.schema.int
import org.ktorm.schema.long
import org.ktorm.schema.varchar
import java.time.LocalDate

object BeatmapTypeTable : AddableTable<BeatmapType>("beatmap_type") {
    val id = int("id").primaryKey().bindTo { it.id }
    val name = varchar("name").bindTo { it.name }
    val triggers = varchar("triggers").bindTo { it.triggers }
    val author = long("authorQq").bindTo { it.author }
    val condition = varchar("condition").bindTo { it.condition }
    val priority = int("priority").bindTo { it.priority }
    val addDate = date("addDate").bindTo { it.addDate }
    val lastEdited = date("lastEdited").bindTo { it.lastEdited }
    val enabled = int("enabled").bindTo { it.enabled }
    val lastError = varchar("lastError").bindTo { it.lastError }

    override fun <T : AssignmentsBuilder> T.mapElement(element: BeatmapType) {
        set(name, element.name)
        set(triggers, element.triggers)
        set(author, element.author)
        set(condition, element.condition)
        set(priority, element.priority)
        set(addDate, element.addDate)
        set(lastEdited, element.lastEdited)
        set(enabled, element.enabled)
        set(lastError, element.lastError)
    }
}

interface BeatmapType : Entity<BeatmapType> {
    companion object : Entity.Factory<BeatmapType>()
    var id: Int
    var name: String
    var triggers: String
    var author: Long
    var condition: String
    var priority: Int
    var addDate: LocalDate
    var lastEdited: LocalDate
    var enabled: Int
    var lastError: String
}