package com.hendraanggrian.openpss.ui.main.help

import com.hendraanggrian.openpss.BuildConfig2
import com.hendraanggrian.openpss.FxComponent
import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.R2
import com.hendraanggrian.openpss.api.GitHubApi
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ktfx.jfoenix.controls.jfxSnackbar

object GitHubHelper {

    fun checkUpdates(component: FxComponent) {
        runBlocking {
            runCatching {
                val release = withContext(Dispatchers.IO) { GitHubApi.getLatestRelease() }
                when {
                    release.isNewerThan(BuildConfig2.VERSION) -> component.rootLayout.jfxSnackbar(
                        component.getString(R2.string.openpss_is_available, release.name),
                        component.getLong(R.value.duration_long),
                        component.getString(R2.string.download)
                    ) {
                        UpdateDialog(component, release.assets).show { url ->
                            component.desktop?.browse(URI(url))
                        }
                    }
                    else -> component.rootLayout.jfxSnackbar(
                        component.getString(
                            R2.string.openpss_is_currently_the_newest_version_available,
                            BuildConfig2.VERSION
                        ),
                        component.getLong(R.value.duration_short)
                    )
                }
            }.onFailure {
                if (BuildConfig2.DEBUG) it.printStackTrace()
                component.rootLayout.jfxSnackbar(
                    component.getString(R2.string.no_internet_connection),
                    component.getLong(R.value.duration_short)
                )
            }
        }
    }
}
