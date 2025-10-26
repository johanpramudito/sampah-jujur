package com.melodi.sampahjujur.ui.screens.collector

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.model.WasteItem
import com.melodi.sampahjujur.ui.components.CollectorBottomNavBar
import com.melodi.sampahjujur.ui.screens.household.StatusBadge
import com.melodi.sampahjujur.ui.screens.household.formatDate
import com.melodi.sampahjujur.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectorDashboardScreen(
    viewModel: com.melodi.sampahjujur.viewmodel.CollectorViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    onRequestClick: (String) -> Unit = {},
    onNavigate: (String) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }

    val pendingRequestsLive by viewModel.pendingRequests.observeAsState(emptyList())
    val myRequests by viewModel.myRequests.observeAsState(emptyList())
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val tabs = listOf("Pending Requests", "My Requests")
    val filterOptions = listOf("All", "Nearest", "Highest Value", "Most Items")

    val pendingRequests = if (uiState.filteredRequests.isNotEmpty() || pendingRequestsLive.isEmpty()) {
        uiState.filteredRequests
    } else {
        pendingRequestsLive
    }
    val searchQuery = uiState.searchQuery
    val selectedFilter = when (uiState.sortBy) {
        "value" -> "Highest Value"
        "weight" -> "Most Items"
        "distance" -> "Nearest"
        else -> "All"
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Dashboard",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        bottomBar = {
            CollectorBottomNavBar(
                selectedRoute = "collector_dashboard",
                onNavigate = onNavigate
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = PrimaryGreen,
                indicator = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (isSelected) PrimaryGreen else Color.Transparent),
                        selectedContentColor = Color.White,
                        unselectedContentColor = PrimaryGreen,
                        text = {
                            Text(
                                text = title,
                                color = if (isSelected) Color.White else PrimaryGreen,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Content based on selected tab
            when (selectedTab) {
                0 -> PendingRequestsTab(
                    requests = pendingRequests,
                    searchQuery = searchQuery,
                    selectedFilter = selectedFilter,
                    filterOptions = filterOptions,
                    isActionInProgress = uiState.isLoading,
                    onSearchQueryChange = { query -> viewModel.filterPendingRequests(query) },
                    onFilterSelect = { filterLabel ->
                        val sortKey = when (filterLabel) {
                            "Highest Value" -> "value"
                            "Most Items" -> "weight"
                            "Nearest" -> "distance"
                            else -> "time"
                        }
                        viewModel.sortPendingRequests(sortKey)
                        viewModel.filterPendingRequests(searchQuery)
                    },
                    onAcceptRequest = { request -> viewModel.acceptPickupRequest(request) },
                    onRequestClick = onRequestClick
                )
                1 -> MyRequestsTab(
                    requests = myRequests,
                    onRequestClick = onRequestClick
                )
            }
        }
    }
}

@Composable
fun PendingRequestsTab(
    requests: List<PickupRequest>,
    searchQuery: String,
    selectedFilter: String,
    filterOptions: List<String>,
    isActionInProgress: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelect: (String) -> Unit,
    onAcceptRequest: (PickupRequest) -> Unit,
    onRequestClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search by location or request ID...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.Gray
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Color.Gray
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = PrimaryGreen,
                unfocusedIndicatorColor = Color.LightGray
            ),
            shape = RoundedCornerShape(28.dp),
            singleLine = true
        )

        // Filter Chips
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filterOptions) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterSelect(filter) },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryGreen,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White
                    ),
                    border = if (selectedFilter == filter) {
                        FilterChipDefaults.filterChipBorder(
                            borderColor = PrimaryGreen,
                            enabled = true,
                            selected = true
                        )
                    } else {
                        FilterChipDefaults.filterChipBorder(
                            borderColor = Color.LightGray,
                            enabled = true,
                            selected = false
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Requests List
        if (requests.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inbox,
                        contentDescription = "No requests",
                        tint = Color.Gray.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Pending Requests",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "New pickup requests will appear here",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(requests) { request ->
                    CollectorRequestCard(
                        request = request,
                        showAcceptButton = true,
                        onCardClick = { onRequestClick(request.id) },
                        onPrimaryAction = { onAcceptRequest(request) },
                        primaryActionEnabled = !isActionInProgress
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}



@Composable
fun MyRequestsTab(
    requests: List<PickupRequest>,
    onRequestClick: (String) -> Unit
) {
    val statusFilters = listOf(
        "All" to null,
        "Accepted" to PickupRequest.STATUS_ACCEPTED,
        "In Progress" to PickupRequest.STATUS_IN_PROGRESS,
        "Completed" to PickupRequest.STATUS_COMPLETED,
        "Cancelled" to PickupRequest.STATUS_CANCELLED
    )

    var selectedFilter by rememberSaveable { mutableStateOf(statusFilters.first().first) }
    var searchQuery by rememberSaveable { mutableStateOf("") }

    val statusFilteredRequests = remember(requests, selectedFilter) {
        val targetStatus = statusFilters.firstOrNull { it.first == selectedFilter }?.second
        if (targetStatus == null) {
            requests
        } else {
            requests.filter { it.status == targetStatus }
        }
    }
    val filteredRequests = remember(statusFilteredRequests, searchQuery) {
        val trimmedQuery = searchQuery.trim()
        if (trimmedQuery.isBlank()) {
            statusFilteredRequests
        } else {
            statusFilteredRequests.filter { request ->
                request.id.contains(trimmedQuery, ignoreCase = true) ||
                request.pickupLocation.address.contains(trimmedQuery, ignoreCase = true) ||
                request.notes.contains(trimmedQuery, ignoreCase = true) ||
                request.wasteItems.any { item ->
                    item.type.contains(trimmedQuery, ignoreCase = true) ||
                    item.description.contains(trimmedQuery, ignoreCase = true)
                }
            }
        }
    }

    if (requests.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(Color(0xFFF5F5F5)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "No accepted requests",
                    tint = Color.Gray.copy(alpha = 0.5f),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Active Requests",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Accepted requests will appear here",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("Search by request ID, address, or notes...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Color.Gray
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = Color.Gray
                            )
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = PrimaryGreen,
                    unfocusedIndicatorColor = Color.LightGray
                ),
                singleLine = true,
                shape = RoundedCornerShape(28.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(statusFilters) { (label, _) ->
                    val isSelected = selectedFilter == label
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = label },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryGreen,
                            selectedLabelColor = Color.White,
                            containerColor = Color.White
                        ),
                        border = if (isSelected) {
                            FilterChipDefaults.filterChipBorder(
                                borderColor = PrimaryGreen,
                                enabled = true,
                                selected = true
                            )
                        } else {
                            FilterChipDefaults.filterChipBorder(
                                borderColor = Color.LightGray,
                                enabled = true,
                                selected = false
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredRequests.isEmpty()) {
                val emptyMessage = when {
                    searchQuery.isNotBlank() -> "No requests match \"$searchQuery\""
                    selectedFilter == "All" -> "No requests yet"
                    else -> "No ${selectedFilter.lowercase()} requests yet"
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No requests for filter",
                            tint = Color.Gray.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = emptyMessage,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    item {
                        Text(
                            text = "${filteredRequests.size} request${if (filteredRequests.size != 1) "s" else ""}",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(filteredRequests) { request ->
                        CollectorRequestCard(
                            request = request,
                            showAcceptButton = false,
                            onCardClick = { onRequestClick(request.id) }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }
            }
        }
    }
}

@Composable
fun CollectorRequestCard(
    request: PickupRequest,
    showAcceptButton: Boolean,
    onCardClick: () -> Unit,
    onPrimaryAction: (() -> Unit)? = null,
    primaryActionEnabled: Boolean = true
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Request #${request.id.take(8).uppercase()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!showAcceptButton) {
                    StatusBadge(status = request.status)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatDate(request.createdAt),
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Location
            Row(
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = PrimaryGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = request.pickupLocation.address,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Items and Weight
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Items",
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${request.wasteItems.size} items - ${request.wasteItems.sumOf { it.weight }} kg",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Estimated Value",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${'$'}${request.wasteItems.sumOf { it.estimatedValue }}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryGreen
                    )
                }

                if (showAcceptButton) {
                    Button(
                        onClick = {
                            if (onPrimaryAction != null) {
                                onPrimaryAction()
                            } else {
                                onCardClick()
                            }
                        },
                        enabled = primaryActionEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryGreen
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Accept",
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                } else {
                    when (request.status) {
                        PickupRequest.STATUS_ACCEPTED -> {
                            OutlinedButton(
                                onClick = onCardClick,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = PrimaryGreen
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = "Navigate",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Navigate", fontWeight = FontWeight.Medium)
                            }
                        }
                        PickupRequest.STATUS_IN_PROGRESS -> {
                            Button(
                                onClick = onCardClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusInProgress
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Complete",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Complete", fontWeight = FontWeight.SemiBold)
                            }
                        }
                        PickupRequest.STATUS_CANCELLED -> {
                            TextButton(onClick = onCardClick) {
                                Text("View Details", color = Color.Gray)
                            }
                        }
                        else -> {
                            TextButton(onClick = onCardClick) {
                                Text("View Details", color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}



// Preview removed - requires ViewModel
// Use Android Studio's interactive preview or run the app to see the UI
