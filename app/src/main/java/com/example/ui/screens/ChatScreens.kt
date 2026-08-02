package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Contacto
import com.example.data.model.Mensaje
import com.example.ui.components.BackgroundGradientCanvas
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.MoneyViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    viewModel: MoneyViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conversations by viewModel.conversations.collectAsState()
    var showNewChatDialog by remember { mutableStateOf(false) }

    ChatListScreen(
        conversations = conversations,
        onSelectConversation = { chat ->
            viewModel.selectActiveChat(chat)
            // Navigate to details
            viewModel.activeChat.value = chat
        },
        onNewChatClick = { showNewChatDialog = true },
        onBack = onBack,
        modifier = modifier
    )

    if (showNewChatDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }

        Dialog(onDismissRequest = { showNewChatDialog = false }) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                cornerRadius = 24.dp,
                backgroundColor = Color(0xF5FFFFFF)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Iniciar Conversación", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del contacto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Teléfono") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    GlassButton(
                        text = "Iniciar",
                        isPrimary = true,
                        onClick = {
                            if (name.isNotBlank() && phone.isNotBlank()) {
                                viewModel.createOrGetContactForChat(name, phone) { created ->
                                    viewModel.selectActiveChat(created)
                                    showNewChatDialog = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = { showNewChatDialog = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Cancelar")
                    }
                }
            }
        }
    }
}

@Composable
fun IndividualChatScreen(
    viewModel: MoneyViewModel,
    conversation: Contacto,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMessages by viewModel.getActiveMessages().collectAsState(initial = emptyList())

    IndividualChatScreen(
        conversation = conversation,
        messages = activeMessages,
        onBack = onBack,
        onSendMessage = { text -> viewModel.sendMessageToChat(conversation.id, text) },
        modifier = modifier
    )
}

// -------------------------------------------------------------
// PANTALLA DE LISTA DE CHATS
// -------------------------------------------------------------
@Composable
fun ChatListScreen(
    conversations: List<Contacto>,
    onSelectConversation: (Contacto) -> Unit,
    onNewChatClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackgroundGradientCanvas(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Mensajería Directa",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.testTag("chat_list_title")
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (conversations.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No hay conversaciones. Toca '+' para agregar.", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(conversations) { chat ->
                            ChatConversationCard(
                                conversation = chat,
                                onClick = { onSelectConversation(chat) }
                            )
                        }

                        item { Spacer(modifier = Modifier.height(100.dp)) }
                    }
                }
            }

            // Botón "Nuevo chat"
            FloatingActionButton(
                onClick = onNewChatClick,
                containerColor = PurplePrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 90.dp, end = 20.dp)
                    .testTag("new_chat_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo Chat")
            }
        }
    }
}

@Composable
fun ChatConversationCard(
    conversation: Contacto,
    onClick: () -> Unit
) {
    val dateStr = if (conversation.hora_ultimo_mensaje != null) {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(conversation.hora_ultimo_mensaje))
    } else {
        ""
    }

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("chat_item_${conversation.id}"),
        cornerRadius = 20.dp,
        backgroundColor = Color(0xF5FFFFFF),
        elevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Foto de perfil circular
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        try {
                            Color(android.graphics.Color.parseColor(conversation.avatarColorHex))
                        } catch (e: Exception) {
                            PurplePrimary
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.nombre.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.nombre,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    )

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (conversation.ultimo_mensaje.orEmpty().length > 30) {
                            conversation.ultimo_mensaje.orEmpty().take(28) + "..."
                        } else {
                            conversation.ultimo_mensaje.orEmpty().ifBlank { "Sin mensajes" }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = TextSecondary,
                            fontSize = 14.sp
                        ),
                        maxLines = 1
                    )

                    if (conversation.mensajes_no_leidos > 0) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${conversation.mensajes_no_leidos}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// PANTALLA DE CHAT INDIVIDUAL
// -------------------------------------------------------------
@Composable
fun IndividualChatScreen(
    conversation: Contacto,
    messages: List<Mensaje>,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val timeFormatter = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    BackgroundGradientCanvas(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCCFFFFFF))
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Volver",
                        tint = TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(conversation.avatarColorHex))
                            } catch (e: Exception) {
                                PurplePrimary
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.nombre.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = conversation.nombre,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "En línea",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }

            // Message Bubbles List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item { Spacer(modifier = Modifier.height(10.dp)) }

                items(messages) { msg ->
                    ChatBubbleItem(message = msg, timeFormatter = timeFormatter)
                }

                item { Spacer(modifier = Modifier.height(10.dp)) }
            }

            // Bottom Input Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x11000000))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("Escribe un mensaje...", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field"),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurplePrimary,
                        unfocusedBorderColor = GlassCardBorder,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    )
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(PurplePrimary)
                        .clickable {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                        .testTag("send_message_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: Mensaje,
    timeFormatter: SimpleDateFormat
) {
    val isUser = message.es_enviado

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp
                        )
                    )
                    .background(if (isUser) PurplePrimary else Color.White)
                    .border(
                        width = if (isUser) 0.dp else 1.dp,
                        color = GlassCardBorder,
                        shape = RoundedCornerShape(18.dp)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.contenido,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (isUser) Color.White else TextPrimary,
                        fontSize = 14.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = timeFormatter.format(Date(message.timestamp)),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            )
        }
    }
}
