package com.example.workmanagerdemo

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.workmanagerdemo.ui.theme.WorkManagerDemoTheme

class MainActivity : ComponentActivity() {
    private val viewModel: BlurViewModel by viewModels {
        BlurViewModel.BlurViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val onSeeFileClick = {
            viewModel.outputUri?.let { currentUri ->
                val actionView = Intent(Intent.ACTION_VIEW, currentUri)
                actionView.resolveActivity(packageManager)?.run {
                    startActivity(actionView)
                }
            }
            Unit
        }

        setContent {
            WorkManagerDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
                        val uiState by viewModel.workStateFlow.collectAsState()

                        MainContents(
                            modifier = Modifier,
                            uiState = uiState,
                            onGoClick = { selectedOption ->
                                getBlurLevel(selectedOption = selectedOption)
                                    .also { blurLevel ->
                                        viewModel.applyBlur(blurLevel)
                                    }
                            },
                            onCancelClick = viewModel::cancelWork,
                            onSeeFileClick = onSeeFileClick
                        )
                    }
                }
            }
        }
    }
}

private val radioOptions = listOf(R.string.blur_lv_1, R.string.blur_lv_2, R.string.blur_lv_3)

private fun getBlurLevel(selectedOption: Int?): Int = when (selectedOption) {
    R.string.blur_lv_1 -> 1
    R.string.blur_lv_2 -> 2
    R.string.blur_lv_3 -> 3
    else -> 1
}

@Composable
fun MainContents(
    modifier: Modifier = Modifier,
    uiState: UiState,
    onGoClick: (Int?) -> Unit,
    onCancelClick: () -> Unit,
    onSeeFileClick: () -> Unit,
) {
    val (selectedOption, onOptionSelected) = remember { mutableStateOf<Int?>(null) }
    var visibilityState by remember { mutableStateOf(InitialVisibilityState) }

    visibilityState = when (uiState.workingState) {
        WorkingState.INITIAL_STATE -> {
            InitialVisibilityState
        }
        WorkingState.IN_PROGRESS -> {
            ProgressVisibilityState
        }
        WorkingState.FINISHED -> {
            visibilityState.changeStateOnFinish(seeFileVisibility = uiState.hasOutputUri)
        }
    }

    Column(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            modifier = Modifier.height(400.dp).fillMaxWidth(),
            painter = painterResource(id = R.drawable.android_cupcake),
            contentDescription = stringResource(id = R.string.description_image)
        )
        Text(
            text = stringResource(id = R.string.blur_title),
            style = MaterialTheme.typography.titleLarge,
        )

        radioOptions.forEach { optionStringResId ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .height(56.dp)
                    .selectable(
                        selected = (selectedOption == optionStringResId),
                        onClick = { onOptionSelected(optionStringResId) },
                        role = Role.RadioButton
                    )
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = (selectedOption == optionStringResId),
                    onClick = null // null recommended for accessibility with screen readers
                )
                Text(
                    modifier = Modifier.padding(start = 16.dp),
                    text = stringResource(id = optionStringResId),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (visibilityState.cancelVisibility) {
                Button(onClick = onCancelClick) {
                    Text(text = stringResource(id = R.string.cancel_work))
                }
            }
            if (visibilityState.progressVisibility) {
                LinearProgressIndicator(
                    modifier = Modifier.width(100.dp).padding(horizontal = 8.dp)
                )
            }
            if (visibilityState.goVisibility) {
                Button(onClick = { onGoClick(selectedOption) }) {
                    Text(text = stringResource(id = R.string.go))
                }
            }
            if (visibilityState.seeFileVisibility) {
                Button(onClick = onSeeFileClick) {
                    Text(text = stringResource(id = R.string.see_file))
                }
            }
        }
    }
}

private data class VisibilityState(
    val progressVisibility: Boolean,
    val cancelVisibility: Boolean,
    val goVisibility: Boolean,
    val seeFileVisibility: Boolean,
)

private val InitialVisibilityState = VisibilityState(
    progressVisibility = false,
    cancelVisibility = false,
    goVisibility = true,
    seeFileVisibility = false,
)

private val ProgressVisibilityState = VisibilityState(
    progressVisibility = true,
    cancelVisibility = true,
    goVisibility = false,
    seeFileVisibility = false,
)

private fun VisibilityState.changeStateOnFinish(seeFileVisibility: Boolean) = copy(
    progressVisibility = false,
    cancelVisibility = false,
    goVisibility = true,
    seeFileVisibility = seeFileVisibility,
)