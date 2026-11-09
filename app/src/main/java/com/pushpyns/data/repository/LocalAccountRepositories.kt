package com.pushpyns.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pushpyns.data.local.EntitlementDao
import com.pushpyns.data.local.EntitlementEntity
import com.pushpyns.data.local.ProfileDao
import com.pushpyns.data.local.ProfileEntity
import com.pushpyns.data.mapper.toStorage
import com.pushpyns.data.mapper.useTypeFromStorage
import com.pushpyns.di.IoDispatcher
import com.pushpyns.domain.model.Profile
import com.pushpyns.domain.model.ThemeMode
import com.pushpyns.domain.repository.ProAccess
import com.pushpyns.domain.repository.ProfileRepository
import com.pushpyns.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.settingsDataStore by preferencesDataStore(name = "pushpyns_settings")

@Singleton
class LocalProfileRepository
@Inject
constructor(private val dao: ProfileDao, @IoDispatcher private val io: CoroutineDispatcher) :
    ProfileRepository {
    override fun observe(): Flow<Profile?> = dao.observe().map { it?.toDomain() }.flowOn(io)

    override suspend fun get(): Profile? = withContext(io) {
        dao.get()?.toDomain()
    }

    override suspend fun save(profile: Profile) = withContext(io) {
        dao.upsert(
            ProfileEntity(
                name = profile.name.trim(),
                businessName = profile.businessName.trim(),
                useType = profile.useType.toStorage(),
            ),
        )
    }

    private fun ProfileEntity.toDomain(): Profile = Profile(name = name, businessName = businessName, useType = useTypeFromStorage(useType))
}

@Singleton
class LocalProAccess
@Inject
constructor(private val dao: EntitlementDao, @IoDispatcher private val io: CoroutineDispatcher) :
    ProAccess {
    override fun observeIsPro(): Flow<Boolean> = dao.observe().map { it?.proEnabled == true }.flowOn(io)

    override suspend fun isPro(): Boolean = withContext(io) {
        dao.get()?.proEnabled == true
    }

    override suspend fun setPreviewEnabled(enabled: Boolean) = withContext(io) {
        dao.upsert(
            EntitlementEntity(
                proEnabled = enabled,
                source = "preview",
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }
}

@Singleton
class DataStoreSettingsRepository
@Inject
constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) : SettingsRepository {
    override val themeMode: Flow<ThemeMode> =
        context.settingsDataStore.data
            .map { prefs ->
                when (prefs[KEY_THEME]) {
                    ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                    ThemeMode.DARK.name -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
            }.flowOn(io)

    override val dynamicColor: Flow<Boolean> =
        context.settingsDataStore.data
            .map { prefs -> prefs[KEY_DYNAMIC] == true }
            .flowOn(io)

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[KEY_THEME] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[KEY_DYNAMIC] = enabled }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC = booleanPreferencesKey("dynamic_color")
    }
}
