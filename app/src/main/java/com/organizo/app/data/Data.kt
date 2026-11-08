package com.organizo.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

/** priority: 0 = Low, 1 = Medium, 2 = High. dueDate = LocalDate epoch day (no time zone issues). */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: Int = 1,
    val dueDate: Long? = null,
    val completed: Boolean = false
)

@Dao
interface NoteDao {
    @Query(
        "SELECT * FROM notes WHERE title LIKE '%' || :q || '%' OR content LIKE '%' || :q || '%' " +
            "ORDER BY pinned DESC, updatedAt DESC"
    )
    fun search(q: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun get(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE notes SET pinned = :pinned WHERE id = :id")
    suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("SELECT COUNT(*) FROM notes")
    fun count(): Flow<Int>
}

@Dao
interface TaskDao {
    @Query(
        "SELECT * FROM tasks ORDER BY completed ASC, " +
            "CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END, dueDate ASC, priority DESC"
    )
    fun all(): Flow<List<Task>>

    @Insert
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 0")
    fun pendingCount(): Flow<Int>
}

@Database(entities = [Note::class, Task::class], version = 1, exportSchema = false)
abstract class OrganizoDb : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile private var instance: OrganizoDb? = null

        fun get(context: Context): OrganizoDb = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, OrganizoDb::class.java, "organizo.db"
            ).build().also { instance = it }
        }
    }
}

/** Theme mode: 0 = system, 1 = light, 2 = dark. */
class SettingsStore private constructor(context: Context) {
    private val prefs = context.getSharedPreferences("organizo_settings", Context.MODE_PRIVATE)
    private val _mode = MutableStateFlow(prefs.getInt("theme_mode", 0))
    val mode: StateFlow<Int> = _mode.asStateFlow()

    fun setMode(m: Int) {
        prefs.edit().putInt("theme_mode", m).apply()
        _mode.value = m
    }

    companion object {
        @Volatile private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore = instance ?: synchronized(this) {
            instance ?: SettingsStore(context.applicationContext).also { instance = it }
        }
    }
}
