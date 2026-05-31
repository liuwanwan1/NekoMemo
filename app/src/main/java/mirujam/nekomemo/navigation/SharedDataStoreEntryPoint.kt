package mirujam.nekomemo.navigation

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import mirujam.nekomemo.ui.shared.SharedDataStore

@EntryPoint
@InstallIn(ActivityComponent::class)
interface SharedDataStoreEntryPoint {
    fun sharedDataStore(): SharedDataStore
}
