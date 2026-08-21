package com.junkfood.seal.ui.page.download

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.DownloadDone
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.NewLabel
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.junkfood.seal.R
import com.junkfood.seal.ui.common.intState
import com.junkfood.seal.ui.common.motion.materialSharedAxisYIn
import com.junkfood.seal.ui.component.ButtonChip
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.DrawerSheetSubtitle
import com.junkfood.seal.ui.component.FilledButtonWithIcon
import com.junkfood.seal.ui.component.OutlinedButtonWithIcon
import com.junkfood.seal.ui.component.SealModalBottomSheet
import com.junkfood.seal.ui.component.SealModalBottomSheetM2
import com.junkfood.seal.ui.component.SegmentedButtonValues
import com.junkfood.seal.ui.component.SingleChoiceSegmentedButton
import com.junkfood.seal.ui.page.command.TemplatePickerDialog
import com.junkfood.seal.ui.page.settings.command.CommandTemplateDialog
import com.junkfood.seal.ui.page.settings.format.AudioQuickSettingsDialog
import com.junkfood.seal.ui.page.settings.format.VideoQualityDialog
import com.junkfood.seal.util.AUDIO_CONVERSION_FORMAT
import com.junkfood.seal.util.AUDIO_CONVERT
import com.junkfood.seal.util.AUDIO_QUALITY
import com.junkfood.seal.util.CONVERT_MP3
import com.junkfood.seal.util.COOKIES
import com.junkfood.seal.util.CUSTOM_COMMAND
import com.junkfood.seal.util.DOWNLOAD_TYPE_INITIALIZATION
import com.junkfood.seal.util.DownloadUtil
import com.junkfood.seal.util.EXTRACT_AUDIO
import com.junkfood.seal.util.FORMAT_COMPATIBILITY
import com.junkfood.seal.util.FORMAT_SELECTION
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.FileUtil.getCookiesFile
import com.junkfood.seal.util.PreferenceStrings
import com.junkfood.seal.util.PreferenceUtil
import com.junkfood.seal.util.PreferenceUtil.getBoolean
import com.junkfood.seal.util.PreferenceUtil.getInt
import com.junkfood.seal.util.PreferenceUtil.updateBoolean
import com.junkfood.seal.util.PreferenceUtil.updateInt
import com.junkfood.seal.util.TEMPLATE_ID
import com.junkfood.seal.util.USE_PREVIOUS_SELECTION
import com.junkfood.seal.util.VIDEO_FORMAT
import com.junkfood.seal.util.VIDEO_QUALITY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DownloadType {
    Audio, Video, Command, None
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSettingDialog(
    useDialog: Boolean = false,
    showDialog: Boolean = false,
    isQuickDownload: Boolean = false,
    onNavigateToCookieGeneratorPage: (String) -> Unit = {},
    onDownloadConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    var videoQuality by VIDEO_QUALITY.intState
    var audioQuality by AUDIO_QUALITY.intState

    var type by remember(showDialog) {
        mutableStateOf(
            when (DOWNLOAD_TYPE_INITIALIZATION.getInt()) {
                USE_PREVIOUS_SELECTION -> {
                    if (CUSTOM_COMMAND.getBoolean()) {
                        DownloadType.Command
                    } else if (EXTRACT_AUDIO.getBoolean()) {
                        DownloadType.Audio
                    } else {
                        DownloadType.Video
                    }
                }
                else -> {
                    DownloadType.Video
                }
            }
        )
    }

    var showAudioSettingsDialog by remember { mutableStateOf(false) }
    var showVideoQualityDialog by remember { mutableStateOf(false) }

    var showTemplateSelectionDialog by remember { mutableStateOf(false) }
    var showTemplateCreatorDialog by remember { mutableStateOf(false) }
    var showTemplateEditorDialog by remember { mutableStateOf(false) }

    val template by remember(
        showTemplateCreatorDialog,
        showTemplateSelectionDialog,
        showTemplateEditorDialog
    ) {
        mutableStateOf(PreferenceUtil.getTemplate())
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(showDialog) {
        if (showDialog) {
            // Ensure cookies are always active, format selection is disabled for direct download,
            // video format is MP4 and audio is converted to MP3
            COOKIES.updateBoolean(true)
            FORMAT_SELECTION.updateBoolean(false)
            VIDEO_FORMAT.updateInt(FORMAT_COMPATIBILITY)
            AUDIO_CONVERT.updateBoolean(true)
            AUDIO_CONVERSION_FORMAT.updateInt(CONVERT_MP3)

            withContext(Dispatchers.IO) {
                DownloadUtil.getCookiesContentFromDatabase().getOrNull()?.let {
                    FileUtil.writeContentToFile(it, context.getCookiesFile())
                }
            }
        }
    }

    val updatePreferences = {
        scope.launch {
            PreferenceUtil.updateValue(EXTRACT_AUDIO, type == DownloadType.Audio)
            PreferenceUtil.updateValue(CUSTOM_COMMAND, type == DownloadType.Command)
            COOKIES.updateBoolean(true)
            FORMAT_SELECTION.updateBoolean(false)
            VIDEO_FORMAT.updateInt(FORMAT_COMPATIBILITY)
            AUDIO_CONVERT.updateBoolean(true)
            AUDIO_CONVERSION_FORMAT.updateInt(CONVERT_MP3)
        }
    }

    val downloadButtonCallback = {
        updatePreferences()
        onDismissRequest()
        onDownloadConfirm()
    }

    val sheetContent: @Composable () -> Unit = {
        Column {
            Text(
                text = stringResource(R.string.settings_before_download_text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            DrawerSheetSubtitle(text = stringResource(id = R.string.download_type))
            Row {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SingleChoiceSegmentedButton(
                        text = stringResource(id = R.string.audio),
                        selected = type == DownloadType.Audio,
                        position = SegmentedButtonValues.START
                    ) {
                        type = DownloadType.Audio
                        updatePreferences()
                    }
                    SingleChoiceSegmentedButton(
                        text = stringResource(id = R.string.video),
                        selected = type == DownloadType.Video
                    ) {
                        type = DownloadType.Video
                        updatePreferences()
                    }
                    SingleChoiceSegmentedButton(
                        text = stringResource(id = R.string.commands),
                        selected = type == DownloadType.Command,
                        position = SegmentedButtonValues.END
                    ) {
                        type = DownloadType.Command
                        updatePreferences()
                    }
                }
            }

            DrawerSheetSubtitle(
                text = stringResource(
                    id = if (type == DownloadType.Command) R.string.template_selection
                    else R.string.quality
                )
            )

            AnimatedContent(
                targetState = type,
                label = "",
                transitionSpec = {
                    (materialSharedAxisYIn(initialOffsetX = { it / 4 })).togetherWith(
                        fadeOut(tween(durationMillis = 80))
                    )
                }
            ) { targetType ->
                when (targetType) {
                    DownloadType.Command -> {
                        LazyRow(modifier = Modifier) {
                            item {
                                ButtonChip(
                                    icon = Icons.Outlined.Code,
                                    label = template.name,
                                    onClick = { showTemplateSelectionDialog = true }
                                )
                            }
                            item {
                                ButtonChip(
                                    icon = Icons.Outlined.NewLabel,
                                    label = stringResource(id = R.string.new_template),
                                    onClick = { showTemplateCreatorDialog = true }
                                )
                            }
                            item {
                                ButtonChip(
                                    icon = Icons.Outlined.Edit,
                                    label = stringResource(
                                        id = R.string.edit_template,
                                        template.name
                                    ),
                                    onClick = { showTemplateEditorDialog = true }
                                )
                            }
                        }
                    }

                    DownloadType.Audio -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            ButtonChip(
                                label = "MP3 • " + PreferenceStrings.getAudioQualityDesc(audioQuality),
                                icon = Icons.Outlined.GraphicEq,
                                enabled = true,
                                iconDescription = stringResource(id = R.string.audio_quality)
                            ) {
                                showAudioSettingsDialog = true
                            }
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            ButtonChip(
                                label = "MP4 • " + PreferenceStrings.getVideoResolutionDescComp(),
                                icon = Icons.Outlined.HighQuality,
                                enabled = true,
                                iconDescription = stringResource(id = R.string.video_quality)
                            ) {
                                showVideoQualityDialog = true
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        @Composable
        fun SheetContent(onDismissRequest: () -> Unit) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Icon(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    imageVector = Icons.Outlined.DoneAll,
                    contentDescription = null
                )
                Text(
                    text = stringResource(R.string.settings_before_download),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 16.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                sheetContent()
                val state = rememberLazyListState()
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    state = state,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    item {
                        OutlinedButtonWithIcon(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            onClick = onDismissRequest,
                            icon = Icons.Outlined.Cancel,
                            text = stringResource(R.string.cancel)
                        )
                    }
                    item {
                        FilledButtonWithIcon(
                            onClick = downloadButtonCallback,
                            icon = Icons.Outlined.DownloadDone,
                            text = stringResource(R.string.start_download),
                            enabled = type != DownloadType.None
                        )
                    }
                }
            }
        }

        if (!useDialog) {
            val useMD2BottomSheet = Build.VERSION.SDK_INT < 30
            if (useMD2BottomSheet) {
                val sheetState = androidx.compose.material.rememberModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Hidden,
                    skipHalfExpanded = true
                )

                BackHandler(sheetState.targetValue == ModalBottomSheetValue.Expanded) {
                    scope.launch {
                        sheetState.hide()
                    }
                }

                LaunchedEffect(Unit) {
                    sheetState.show()
                }

                LaunchedEffect(sheetState.isVisible) {
                    if (sheetState.targetValue == ModalBottomSheetValue.Hidden) {
                        onDismissRequest()
                    }
                }

                SealModalBottomSheetM2(
                    sheetState = sheetState,
                    horizontalPadding = PaddingValues(horizontal = 20.dp),
                    sheetContent = {
                        SheetContent(onDismissRequest = {
                            scope.launch {
                                sheetState.hide()
                            }
                        })
                    }
                )
            } else {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
                val onSheetDismiss: () -> Unit = {
                    scope.launch {
                        sheetState.hide()
                    }.invokeOnCompletion { onDismissRequest() }
                }

                SealModalBottomSheet(
                    sheetState = sheetState,
                    horizontalPadding = PaddingValues(horizontal = 20.dp),
                    onDismissRequest = onDismissRequest,
                    content = {
                        SheetContent(onDismissRequest = onSheetDismiss)
                    }
                )
            }
        } else {
            AlertDialog(
                onDismissRequest = onDismissRequest,
                confirmButton = {
                    TextButton(onClick = downloadButtonCallback) {
                        Text(text = stringResource(R.string.start_download))
                    }
                },
                dismissButton = { DismissButton { onDismissRequest() } },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.DoneAll,
                        contentDescription = null
                    )
                },
                title = {
                    Text(
                        stringResource(R.string.settings_before_download),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        sheetContent()
                    }
                }
            )
        }
    }

    if (showAudioSettingsDialog) {
        AudioQuickSettingsDialog(onDismissRequest = { showAudioSettingsDialog = false })
    }

    if (showVideoQualityDialog) {
        VideoQualityDialog(
            videoQuality = videoQuality,
            onDismissRequest = { showVideoQualityDialog = false },
            onConfirm = {
                VIDEO_QUALITY.updateInt(it)
                videoQuality = it
            }
        )
    }

    if (showTemplateSelectionDialog) {
        TemplatePickerDialog { showTemplateSelectionDialog = false }
    }

    if (showTemplateCreatorDialog) {
        CommandTemplateDialog(
            onDismissRequest = { showTemplateCreatorDialog = false },
            confirmationCallback = {
                scope.launch {
                    TEMPLATE_ID.updateInt(it)
                }
            }
        )
    }

    if (showTemplateEditorDialog) {
        CommandTemplateDialog(
            commandTemplate = template,
            onDismissRequest = { showTemplateEditorDialog = false }
        )
    }
}