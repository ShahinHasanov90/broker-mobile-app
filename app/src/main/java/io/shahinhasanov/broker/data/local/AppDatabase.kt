package io.shahinhasanov.broker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import io.shahinhasanov.broker.data.model.DeclarationStatus

class StatusConverter {
    @TypeConverter
    fun toStatus(raw: String): DeclarationStatus = DeclarationStatus.valueOf(raw)

    @TypeConverter
    fun fromStatus(status: DeclarationStatus): String = status.name
}

class OutboxOperationConverter {
    @TypeConverter
    fun toOperation(raw: String): OutboxOperation = OutboxOperation.valueOf(raw)

    @TypeConverter
    fun fromOperation(op: OutboxOperation): String = op.name
}

class OutboxStateConverter {
    @TypeConverter
    fun toState(raw: String): OutboxState = OutboxState.valueOf(raw)

    @TypeConverter
    fun fromState(state: OutboxState): String = state.name
}

@Database(
    entities = [DeclarationEntity::class, OutboxEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(StatusConverter::class, OutboxOperationConverter::class, OutboxStateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun declarationDao(): DeclarationDao
    abstract fun outboxDao(): OutboxDao

    companion object {
        const val NAME = "broker.db"
    }
}
