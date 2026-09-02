package com.novatube.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.novatube.app.NovaTubeApp

class ViewModelFactory(private val app: NovaTubeApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when (modelClass) {
            DownloadsViewModel::class.java -> DownloadsViewModel(app) as T
            SearchViewModel::class.java -> SearchViewModel(app) as T
            LibraryViewModel::class.java -> LibraryViewModel(app) as T
            BrowserViewModel::class.java -> BrowserViewModel(app) as T
            FormatSelectionViewModel::class.java -> FormatSelectionViewModel(app) as T
            SettingsViewModel::class.java -> SettingsViewModel(app) as T
            HomeViewModel::class.java -> HomeViewModel(app) as T
            PlaylistsViewModel::class.java -> PlaylistsViewModel(app) as T
            else -> super.create(modelClass, extras)
        }
    }

    companion object {
        fun from(context: Context): ViewModelFactory = ViewModelFactory(context.applicationContext as NovaTubeApp)
    }
}
