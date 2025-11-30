package com.melodi.sampahjujur.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.melodi.sampahjujur.model.Chat
import com.melodi.sampahjujur.model.Message
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.repository.ChatRepository
import com.melodi.sampahjujur.repository.WasteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val wasteRepository: WasteRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    // Current chat being viewed
    private val _currentChat = MutableStateFlow<Chat?>(null)
    val currentChat: StateFlow<Chat?> = _currentChat.asStateFlow()

    // Messages in the current chat
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    // All chats for the current user
    private val _chats = MutableStateFlow<List<Chat>>(emptyList())
    val chats: StateFlow<List<Chat>> = _chats.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Total unread count across all chats
    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()

    // Request status for current chat
    private val _requestStatus = MutableStateFlow<String?>(null)
    val requestStatus: StateFlow<String?> = _requestStatus.asStateFlow()

    // Helper to check if chat is read-only (request is completed or cancelled)
    val isChatReadOnly: StateFlow<Boolean> = _requestStatus.map { status ->
        status == PickupRequest.STATUS_COMPLETED || status == PickupRequest.STATUS_CANCELLED
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    init {
        observeChats()
    }

    /**
     * Observe all chats for the current user
     */
    private fun observeChats() {
        if (currentUserId.isBlank()) return

        viewModelScope.launch {
            chatRepository.observeChatsForUser(currentUserId)
                .catch { e ->
                    _error.value = e.message ?: "Failed to load chats"
                }
                .collect { chatList ->
                    _chats.value = chatList
                    // Calculate total unread count
                    _totalUnreadCount.value = chatList.sumOf { it.getUnreadCount(currentUserId) }
                }
        }
    }

    /**
     * Load a specific chat and its messages
     */
    fun loadChat(chatId: String) {
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "loadChat called for chatId: $chatId")
            _isLoading.value = true
            _error.value = null

            // Get chat details
            chatRepository.getChatById(chatId).onSuccess { chat ->
                android.util.Log.d("ChatViewModel", "Chat loaded successfully: $chatId")
                _currentChat.value = chat
                _isLoading.value = false

                // Mark messages as read
                chatRepository.markMessagesAsRead(chatId, currentUserId)

                // Observe messages in a separate coroutine
                viewModelScope.launch {
                    android.util.Log.d("ChatViewModel", "Starting to observe messages for chatId: $chatId")
                    chatRepository.observeMessages(chatId)
                        .catch { e ->
                            android.util.Log.e("ChatViewModel", "Error in message flow for chatId: $chatId", e)
                            _error.value = e.message ?: "Failed to load messages"
                        }
                        .collect { messageList ->
                            android.util.Log.d("ChatViewModel", "Messages collected: ${messageList.size} messages")
                            _messages.value = messageList
                        }
                }

                // Observe request status
                if (chat.requestId.isNotBlank()) {
                    viewModelScope.launch {
                        android.util.Log.d("ChatViewModel", "Starting to observe request status for requestId: ${chat.requestId}")
                        wasteRepository.observeRequest(chat.requestId)
                            .catch { e ->
                                android.util.Log.e("ChatViewModel", "Error observing request status", e)
                            }
                            .collect { request ->
                                _requestStatus.value = request?.status
                                android.util.Log.d("ChatViewModel", "Request status updated: ${request?.status}")
                            }
                    }
                }
            }.onFailure { e ->
                android.util.Log.e("ChatViewModel", "Failed to load chat $chatId", e)
                _error.value = e.message ?: "Failed to load chat"
                _isLoading.value = false
            }
        }
    }

    /**
     * Load chat by request ID
     */
    fun loadChatByRequestId(requestId: String) {
        viewModelScope.launch {
            android.util.Log.d("ChatViewModel", "loadChatByRequestId called for requestId: $requestId")
            _isLoading.value = true
            _error.value = null

            try {
                chatRepository.getChatByRequestId(requestId).onSuccess { chat ->
                    if (chat != null) {
                        android.util.Log.d("ChatViewModel", "Chat found for requestId $requestId, chatId: ${chat.id}")
                        loadChat(chat.id)
                    } else {
                        android.util.Log.w("ChatViewModel", "No chat found for requestId: $requestId")
                        _error.value = "Chat is being set up. Please try again in a moment."
                        _isLoading.value = false
                    }
                }.onFailure { e ->
                    android.util.Log.e("ChatViewModel", "Failed to get chat by requestId: $requestId", e)
                    _error.value = e.message ?: "Failed to load chat"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Exception in loadChatByRequestId for $requestId", e)
                _error.value = e.message ?: "Failed to load chat"
                _isLoading.value = false
            }
        }
    }

    /**
     * Send a message
     */
    fun sendMessage(text: String) {
        val chat = _currentChat.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            _isSending.value = true
            _error.value = null

            try {
                // Get sender name from current chat
                val senderName = when (currentUserId) {
                    chat.householdId -> chat.householdName
                    chat.collectorId -> chat.collectorName
                    else -> "Unknown"
                }

                val message = Message(
                    chatId = chat.id,
                    senderId = currentUserId,
                    senderName = senderName,
                    text = text.trim(),
                    timestamp = System.currentTimeMillis(),
                    read = false,
                    type = Message.MessageType.TEXT
                )

                chatRepository.sendMessage(chat.id, message).onFailure { e ->
                    _error.value = e.message ?: "Failed to send message"
                }
            } finally {
                _isSending.value = false
            }
        }
    }

    /**
     * Mark current chat as read
     */
    fun markAsRead() {
        val chat = _currentChat.value ?: return

        viewModelScope.launch {
            chatRepository.markMessagesAsRead(chat.id, currentUserId)
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Clear current chat (when navigating away)
     */
    fun clearCurrentChat() {
        _currentChat.value = null
        _messages.value = emptyList()
        _requestStatus.value = null
    }

    /**
     * Get unread count for a specific chat
     */
    fun getUnreadCount(chatId: String): Int {
        return _chats.value.find { it.id == chatId }?.getUnreadCount(currentUserId) ?: 0
    }
}
