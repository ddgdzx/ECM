package com.ecm.inventory.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ComponentDao {

    @Query("SELECT * FROM components ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components WHERE id = :id")
    fun observeById(id: Long): Flow<ComponentEntity?>

    @Query("SELECT * FROM components WHERE locationId = :locationId")
    fun observeByLocation(locationId: Long): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components WHERE locationId = :locationId")
    suspend fun findByLocation(locationId: Long): List<ComponentEntity>

    @Query("SELECT * FROM components WHERE id = :id")
    suspend fun findById(id: Long): ComponentEntity?

    @Query("SELECT COUNT(*) FROM components")
    suspend fun count(): Int

    @Query("SELECT * FROM components")
    suspend fun findAll(): List<ComponentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ComponentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ComponentEntity>)

    @Query("DELETE FROM components")
    suspend fun clear()

    @Update
    suspend fun update(item: ComponentEntity)

    @Delete
    suspend fun delete(item: ComponentEntity)

    @Query("UPDATE components SET quantity = :quantity, updatedAt = :now WHERE id = :id")
    suspend fun setQuantity(id: Long, quantity: Int, now: Long = System.currentTimeMillis())

    /** 位置被删除后，把里面的元件标记为“未分配”。 */
    @Query("UPDATE components SET locationId = NULL, slotsData = '' WHERE locationId = :locationId")
    suspend fun detachFromLocation(locationId: Long)

    /** 容器缩小后，把落在范围外的元件移出格口。 */
    @Query(
        "UPDATE components SET locationId = NULL WHERE locationId = :locationId " +
            "AND (layer >= :layers OR row >= :rows OR col >= :cols)"
    )
    suspend fun detachOutOfRange(locationId: Long, layers: Int, rows: Int, cols: Int)
}

@Dao
interface ConsumptionDao {
    @Query("SELECT * FROM consumption_records ORDER BY consumedAt DESC")
    fun observeAll(): Flow<List<ConsumptionEntity>>

    @Query("SELECT * FROM consumption_records")
    suspend fun findAll(): List<ConsumptionEntity>

    @Insert
    suspend fun insert(item: ConsumptionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ConsumptionEntity>)

    @Query("DELETE FROM consumption_records")
    suspend fun clear()

    @Query("DELETE FROM consumption_records WHERE componentId = :componentId")
    suspend fun deleteByComponent(componentId: Long)
}

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    fun observeById(id: Long): Flow<LocationEntity?>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun findById(id: Long): LocationEntity?

    @Query("SELECT * FROM locations")
    suspend fun findAll(): List<LocationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LocationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LocationEntity>)

    @Query("DELETE FROM locations")
    suspend fun clear()

    @Update
    suspend fun update(item: LocationEntity)

    @Delete
    suspend fun delete(item: LocationEntity)
}

@Database(
    entities = [ComponentEntity::class, LocationEntity::class, ConsumptionEntity::class],
    version = 2,
    exportSchema = false
)
abstract class EcmDatabase : RoomDatabase() {

    abstract fun componentDao(): ComponentDao
    abstract fun locationDao(): LocationDao
    abstract fun consumptionDao(): ConsumptionDao

    companion object {
        @Volatile
        private var instance: EcmDatabase? = null

        fun get(context: Context): EcmDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                EcmDatabase::class.java,
                "ecm.db"
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE components ADD COLUMN slotsData TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS consumption_records (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "componentId INTEGER NOT NULL, quantity INTEGER NOT NULL, " +
                        "detail TEXT NOT NULL, consumedAt INTEGER NOT NULL, stockAfter INTEGER NOT NULL)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_consumption_records_componentId ON consumption_records(componentId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_consumption_records_consumedAt ON consumption_records(consumedAt)")
            }
        }
    }
}
