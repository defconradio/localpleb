package com.example.pleb2.ui
//TODO this screen must navigate back always to the OrderBookScreen ?
// what if this screen open from product detail ? ux work here

//TODO w: OrderScreen.kt 'val LocalClipboardManager: ProvidableCompositionLocal<ClipboardManager>' is deprecated. Use LocalClipboard instead which supports suspend functions.
// list all compose and related libs version find the last stable for all
// same apply for ProductDetailScreen.kt MyStallScreen.kt
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.pleb2.R
import com.example.data.uiModels.OrderUiModel
import com.example.data.uiModels.MessageStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(
    navController: NavController,
    // setBottomBarVisibility: (Boolean) -> Unit
) {
    val viewModel: AndroidOrderViewModel = hiltViewModel()
    var messageText by remember { mutableStateOf("") }
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isInitialLoad by viewModel.isInitialLoad.collectAsState()
    val error by viewModel.error.collectAsState()
    //val eoseHit by viewModel.eoseHit.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var messageToDelete by remember { mutableStateOf<OrderUiModel?>(null) }
    /*
    DisposableEffect(Unit) {
        setBottomBarVisibility(false)
        onDispose {
            setBottomBarVisibility(true)
        }
    }
    */

    /*
    LaunchedEffect(viewModel) {
        viewModel.subscribeToMessages()
    }
    */

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Pleb Chat")
                        // Text(
                        //     text = "EOSE: $eoseHit",
                        //     style = MaterialTheme.typography.bodySmall,
                        //     color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        // )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()

            //.safeDrawingPadding() //this not work move the loading indicator far down

        ) {
            // Show loading indicator above Snackbar
            Box(modifier = Modifier.height(4.dp).fillMaxWidth()) {
                if (isLoading) {
                    LinearProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (error != null) {
                val clipboardManager = LocalClipboardManager.current
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            /*
                            TextButton(onClick = { viewModel.subscribeToMessages() }) {
                                Text("Retry", color = MaterialTheme.colorScheme.onError)
                            }
                            */
                            TextButton(onClick = { viewModel.clearError() }) {
                                Text("Dismiss", color = MaterialTheme.colorScheme.onError)
                            }
                            TextButton(onClick = {
                                clipboardManager.setText(AnnotatedString(error ?: ""))
                            }) {
                                Text("Copy", color = MaterialTheme.colorScheme.onError)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.foundation.text.selection.SelectionContainer {
                            Text(
                                error ?: "Unknown error",
                                color = MaterialTheme.colorScheme.onError
                            )
                        }
                    }
                }
            }
            // Chat messages area
            if (!isInitialLoad) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { msg ->
                        val isDeleting = msg.isDeleting
                        val isSending = msg.status == MessageStatus.SENDING
                        Box(
                            modifier = Modifier.combinedClickable(
                                enabled = !isDeleting,
                                onClick = {},
                                onLongClick = {
                                    if (msg.isFromCurrentUser) {
                                        messageToDelete = msg
                                        showDeleteDialog = true
                                    }
                                }
                            ).alpha(if (isDeleting || isSending) 0.5f else 1f)
                        ) {
                            MessageBubble(
                                message = msg,
                                onLongPress = {
                                    if (msg.isFromCurrentUser && !isDeleting) {
                                        messageToDelete = msg
                                        showDeleteDialog = true
                                    }
                                },
                                onRetry = { viewModel.resendMessage(msg.event_id) }
                            )
                        }
                    }
                }
            } else {
                // Keep the space occupied while loading to prevent the input field from jumping
                Spacer(modifier = Modifier.weight(1f))
            }


            // Input field and send button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .imePadding(), // This will push the input field up
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    // viewModel.sendMessage(messageText)
                    viewModel.sendKind1059Message(messageText)
                    messageText = ""
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send Message"
                    )
                }
            }
        }
    }
    if (showDeleteDialog && messageToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Message") },
            text = { Text("Are you sure you want to delete this message?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteMessage(messageToDelete!!.event_id)
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MessageBubble(
    message: OrderUiModel,
    onLongPress: (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    val bubbleColor = if (message.isFromCurrentUser) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val horizontalArrangement = if (message.isFromCurrentUser) Arrangement.End else Arrangement.Start

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(
                onClick = {},
                onLongClick = { onLongPress?.invoke() }
            ),
        horizontalArrangement = horizontalArrangement,
        //verticalAlignment = Alignment.Bottom
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = bubbleColor,
            tonalElevation = 1.dp,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { onLongPress?.invoke() }
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Status icon for messages from the current user
        if (message.isFromCurrentUser) {
            Spacer(modifier = Modifier.width(4.dp))
            when (message.status) {
                /*
                MessageStatus.SENDING -> Icon(
                    imageVector = Icons.Outlined.Schedule,
                    contentDescription = "Sending",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                */
                MessageStatus.FAILED -> Icon(
                    imageVector = Icons.Filled.Error,
                    contentDescription = "Failed to send",
                    modifier = Modifier
                        .size(16.dp)
                        .combinedClickable { onRetry?.invoke() },
                    tint = MaterialTheme.colorScheme.error
                )
                /*
                MessageStatus.SENT -> Icon(
                    imageVector = Icons.Filled.Done,
                    contentDescription = "Sent",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                */
                else -> {
                    // Do nothing for other states
                }
            }
        }
    }
}