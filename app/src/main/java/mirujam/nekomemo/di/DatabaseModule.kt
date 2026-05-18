package mirujam.nekomemo.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import androidx.room.migration.Migration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import mirujam.nekomemo.data.local.NekoMemoDatabase
import mirujam.nekomemo.data.local.dao.QuestionBankDao
import mirujam.nekomemo.data.local.dao.QuestionDao
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val MIGRATIONS: Array<Migration> = arrayOf()

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): NekoMemoDatabase {
        return Room.databaseBuilder(
            context,
            NekoMemoDatabase::class.java,
            "nekomemo_database"
        )
            .fallbackToDestructiveMigrationOnDowngrade(false)
            .addMigrations(*MIGRATIONS)
            .build()
    }

    @Provides
    fun provideQuestionBankDao(database: NekoMemoDatabase): QuestionBankDao =
        database.questionBankDao()

    @Provides
    fun provideQuestionDao(database: NekoMemoDatabase): QuestionDao =
        database.questionDao()

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = context.dataStore
}
