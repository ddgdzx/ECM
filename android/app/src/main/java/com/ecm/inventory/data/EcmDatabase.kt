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
import kotlinx.coroutines.flow.Flow

@Dao
interface ComponentDao {

    @Query("SELECT * FROM components ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<ComponentEntity>>

    @Query("SELECT * FROM components WHERE id = :id")
    fun observeById(id: Long): Flow<ComponentEntity?>

    @Query("SELECT * FROM components WHERE locationId = :locationId")
    fun observeByLocation(locationId: Long): Flow<List<ComponentEntity>>

    @Query("SELECT COUNT(*) FROM components")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ComponentEntity): Long

    @Update
    suspend fun update(item: ComponentEntity)

    @Delete
    suspend fun delete(item: ComponentEntity)

    @Query("UPDATE components SET quantity = :quantity, updatedAt = :now WHERE id = :id")
    suspend fun setQuantity(id: Long, quantity: Int, now: Long = System.currentTimeMillis())

    /** 位置被删除后，把里面的元件标记为“未分配”。 */
    @Query("UPDATE components SET locationId = NULL WHERE locationId = :locationId")
    suspend fun detachFromLocation(locationId: Long)

    /** 容器缩小后，把落在范围外的元件移出格口。 */
    @Query(
        "UPDATE components SET locationId = NULL WHERE locationId = :locationId " +
            "AND (layer >= :layers OR row >= :rows OR col >= :cols)"
    )
    suspend fun detachOutOfRange(locationId: Long, layers: Int, rows: Int, cols: Int)
}

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    fun observeById(id: Long): Flow<LocationEntity?>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun findById(id: Long): LocationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LocationEntity): Long

    @Update
    suspend fun update(item: LocationEntity)

    @Delete
    suspend fun delete(item: LocationEntity)
}

@Database(
    entities = [ComponentEntity::class, LocationEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EcmDatabase : RoomDatabase() {

    abstract fun componentDao(): ComponentDao
    abstract fun locationDao(): LocationDao

    companion object {
        @Volatile
        private var instance: EcmDatabase? = null

        fun get(context: Context): EcmDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                EcmDatabase::class.java,
                "ecm.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
