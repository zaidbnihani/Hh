package com.junkfood.seal.ui.page.settings.about

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.junkfood.seal.App
import com.junkfood.seal.App.Companion.packageInfo
import com.junkfood.seal.R
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.LargeTopAppBar
import com.junkfood.seal.ui.component.PreferenceItem
import com.junkfood.seal.util.JsRuntimeUtil
import com.junkfood.seal.util.ToastUtil

private const val releaseURL = "https://github.com/JunkFood02/Seal/releases"
private const val repoUrl = "https://github.com/JunkFood02/Seal"
private const val moreAppsUrl = "https://github.com/JunkFood02?tab=repositories"
private const val privacyPolicyUrl = "https://zaidmtsmbanihani.blogspot.com/2026/08/seal.html"
private const val termsOfUseUrl = "https://zaidmtsmbnyhany.blogspot.com/2026/08/seal.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutPage(
    onNavigateBack: () -> Unit,
    onNavigateToCreditsPage: () -> Unit = {},
    onNavigateToUpdatePage: () -> Unit = {},
    onNavigateToDonatePage: () -> Unit = {}
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true }
    )
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    val info = App.getVersionReport()
    val versionName = packageInfo.versionName
    val diagnostics = JsRuntimeUtil.getDiagnostics(context)

    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(text = stringResource(id = R.string.about))
                },
                navigationIcon = {
                    BackButton {
                        onNavigateBack()
                    }
                },
                scrollBehavior = scrollBehavior
            )
        },
        content = { innerPadding ->
            LazyColumn(modifier = Modifier.padding(innerPadding)) {
                // Version Item
                item {
                    PreferenceItem(
                        title = stringResource(R.string.version),
                        description = "$versionName • ${diagnostics.primaryAbi}",
                        icon = Icons.Outlined.Info,
                    ) {
                        clipboardManager.setText(AnnotatedString(info))
                        ToastUtil.makeToast(R.string.info_copied)
                    }
                }

                // System & Component Diagnostics
                item {
                    PreferenceItem(
                        title = stringResource(R.string.system_diagnostics),
                        description = "Engine: ${diagnostics.runtimeName} | EJS: ${if (diagnostics.isEjsSupported) "Active" else "Inactive"}",
                        icon = Icons.Outlined.Memory,
                    ) {
                        val diagReport = StringBuilder()
                            .append(info).append("\n")
                            .append("JS Runtime: ${diagnostics.runtimeName}\n")
                            .append("JS Path: ${diagnostics.jsRuntimePath ?: "N/A"}\n")
                            .append("EJS Supported: ${diagnostics.isEjsSupported}")
                            .toString()
                        clipboardManager.setText(AnnotatedString(diagReport))
                        ToastUtil.makeToast(R.string.info_copied)
                    }
                }

                // Privacy Policy (سياسة الخصوصية)
                item {
                    PreferenceItem(
                        title = stringResource(R.string.privacy_policy),
                        description = stringResource(R.string.privacy_policy_desc),
                        icon = Icons.Outlined.PrivacyTip,
                    ) {
                        openUrl(privacyPolicyUrl)
                    }
                }

                // Terms of Use (شروط الاستخدام)
                item {
                    PreferenceItem(
                        title = stringResource(R.string.terms_of_service),
                        description = stringResource(R.string.terms_of_service_desc),
                        icon = Icons.Outlined.Description,
                    ) {
                        openUrl(termsOfUseUrl)
                    }
                }

                // GitHub Repository
                item {
                    PreferenceItem(
                        title = stringResource(R.string.github_repo),
                        description = repoUrl,
                        icon = Icons.Outlined.Code,
                    ) {
                        openUrl(repoUrl)
                    }
                }

                // Releases & Changelogs
                item {
                    PreferenceItem(
                        title = stringResource(R.string.release),
                        description = stringResource(R.string.release_desc),
                        icon = Icons.Outlined.NewReleases,
                    ) {
                        openUrl(releaseURL)
                    }
                }

                // More of our apps
                item {
                    PreferenceItem(
                        title = stringResource(R.string.more_apps),
                        description = stringResource(R.string.more_apps_desc),
                        icon = Icons.Outlined.Apps,
                    ) {
                        openUrl(moreAppsUrl)
                    }
                }
            }
        }
    )
}
