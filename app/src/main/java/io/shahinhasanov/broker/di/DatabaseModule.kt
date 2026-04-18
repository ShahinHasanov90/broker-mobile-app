package io.shahinhasanov.broker.di

import android.content.Context
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.shahinhasanov.broker.data.local.AppDatabase
import io.shahinhasanov.broker.data.local.DeclarationDao
import io.shahinhasanov.broker.data.local.JsonCodec
import io.shahinhasanov.broker.data.local.OutboxDao
import io.shahinhasanov.broker.data.model.DeclarationLine
import io.shahinhasanov.broker.data.model.StatusEvent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDeclarationDao(db: AppDatabase): DeclarationDao = db.declarationDao()

    @Provides
    fun provideOutboxDao(db: AppDatabase): OutboxDao = db.outboxDao()

    @Provides
    @Singleton
    fun provideJsonCodec(): JsonCodec {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val lineAdapter = moshi.adapter<List<DeclarationLine>>(
            Types.newParameterizedType(List::class.java, DeclarationLine::class.java)
        )
        val stringAdapter = moshi.adapter<List<String>>(
            Types.newParameterizedType(List::class.java, String::class.java)
        )
        val eventAdapter = moshi.adapter<List<StatusEvent>>(
            Types.newParameterizedType(List::class.java, StatusEvent::class.java)
        )
        return object : JsonCodec {
            override fun encodeLines(value: List<DeclarationLine>) = lineAdapter.toJson(value)
            override fun decodeLines(raw: String) = lineAdapter.fromJson(raw) ?: emptyList()
            override fun encodeAttachments(value: List<String>) = stringAdapter.toJson(value)
            override fun decodeAttachments(raw: String) = stringAdapter.fromJson(raw) ?: emptyList()
            override fun encodeHistory(value: List<StatusEvent>) = eventAdapter.toJson(value)
            override fun decodeHistory(raw: String) = eventAdapter.fromJson(raw) ?: emptyList()
        }
    }
}
