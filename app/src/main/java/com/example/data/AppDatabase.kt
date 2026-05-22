package com.example.data

import android.content.Context
import androidx.room.*
import com.example.domain.CanvasStroke
import com.example.domain.ThreeDObject
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val strokeListType = Types.newParameterizedType(List::class.java, CanvasStroke::class.java)
    private val strokeAdapter = moshi.adapter<List<CanvasStroke>>(strokeListType)

    private val objectListType = Types.newParameterizedType(List::class.java, ThreeDObject::class.java)
    private val objectAdapter = moshi.adapter<List<ThreeDObject>>(objectListType)

    @TypeConverter
    fun fromStrokeList(strokes: List<CanvasStroke>?): String {
        return strokes?.let { strokeAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toStrokeList(json: String?): List<CanvasStroke> {
        return json?.let { strokeAdapter.fromJson(it) } ?: emptyList()
    }

    @TypeConverter
    fun fromObjectList(objects: List<ThreeDObject>?): String {
        return objects?.let { objectAdapter.toJson(it) } ?: "[]"
    }

    @TypeConverter
    fun toObjectList(json: String?): List<ThreeDObject> {
        return json?.let { objectAdapter.fromJson(it) } ?: emptyList()
    }
}

@Database(
    entities = [FolderEntity::class, NoteFileEntity::class, PageAnnotationEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun noteFileDao(): NoteFileDao
    abstract fun pageAnnotationDao(): PageAnnotationDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "auranotes_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
