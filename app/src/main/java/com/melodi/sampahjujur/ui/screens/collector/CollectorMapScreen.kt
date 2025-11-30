package com.melodi.sampahjujur.ui.screens.collector

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.BottomSheetScaffold
import androidx.compose.material.BottomSheetValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material.rememberBottomSheetState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.viewinterop.AndroidView
import com.melodi.sampahjujur.model.PickupRequest
import com.melodi.sampahjujur.ui.components.CollectorBottomNavBar
import com.melodi.sampahjujur.ui.screens.household.StatusBadge
import com.melodi.sampahjujur.ui.screens.household.formatDate
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.viewmodel.CollectorViewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun CollectorMapScreen(
    viewModel: CollectorViewModel = hiltViewModel(),
    onNavigate: (String) -> Unit,
    onRequestSelected: (String) -> Unit
) {
    val mapState by viewModel.mapState.collectAsState()
    val pendingRequests by viewModel.pendingRequests.observeAsState(emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.setZoom(13.0)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                mapView.onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                mapView.onPause()
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            mapView.onPause()
        }
    }

    val requestMarkerIcon: Drawable? = remember {
        AppCompatResources.getDrawable(context, com.melodi.sampahjujur.R.drawable.ic_request_marker)
    }
    val collectorMarkerIcon: Drawable? = remember {
        AppCompatResources.getDrawable(context, com.melodi.sampahjujur.R.drawable.ic_collector_location)
    }

    var selectedRequestId by rememberSaveable { mutableStateOf<String?>(null) }
    var shouldCenterCollector by rememberSaveable { mutableStateOf(true) }
    val bottomSheetState = rememberBottomSheetState(
        initialValue = BottomSheetValue.Collapsed
    )
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )
    val sortedRequests = remember(pendingRequests, mapState.collectorLocation) {
        sortRequestsByDistance(
            requests = pendingRequests,
            collectorLocation = mapState.collectorLocation
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val isSheetCollapsed by remember {
        derivedStateOf { bottomSheetState.currentValue == BottomSheetValue.Collapsed }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshCollectorLocation()
    }

    LaunchedEffect(mapState.errorMessage) {
        mapState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearMapError()
        }
    }

    LaunchedEffect(mapState.collectorLocation, shouldCenterCollector) {
        if (shouldCenterCollector) {
            mapState.collectorLocation?.let { location ->
                val point = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(point, 15.0, 1000L)
                shouldCenterCollector = false
            }
        }
    }

    LaunchedEffect(pendingRequests.isEmpty()) {
        if (pendingRequests.isEmpty()) {
            bottomSheetState.collapse()
        }
    }

    LaunchedEffect(selectedRequestId, mapState.pendingMarkers) {
        selectedRequestId?.let { requestId ->
            mapState.pendingMarkers.firstOrNull { it.requestId == requestId }?.let { markerInfo ->
                val point = GeoPoint(markerInfo.latitude, markerInfo.longitude)
                mapView.controller.animateTo(point, 16.0, 1000L)
            }
        }
    }

    val navBarHeight = 72.dp
    val collapsedHeaderHeight = navBarHeight + 120.dp // Increased to show more info by default

    Box(modifier = Modifier.fillMaxSize()) {
        BottomSheetScaffold(
            scaffoldState = bottomSheetScaffoldState,
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Pickup Map", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            sheetPeekHeight = if (pendingRequests.isEmpty()) 0.dp else collapsedHeaderHeight,
            sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            sheetElevation = 8.dp,
            sheetBackgroundColor = Color.White,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            backgroundColor = Color(0xFFF5F5F5),
            sheetContent = {
                if (pendingRequests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No pending requests nearby",
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 750.dp) // Expanded height - stops just before title bar
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(bottom = navBarHeight + 48.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(48.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.LightGray.copy(alpha = 0.6f))
                            .align(Alignment.CenterHorizontally)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "Nearby Requests",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${sortedRequests.size} pending nearby",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    if (isSheetCollapsed) bottomSheetState.expand() else bottomSheetState.collapse()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSheetCollapsed) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isSheetCollapsed) "Expand" else "Collapse",
                                tint = PrimaryGreen
                            )
                        }
                    }

                    if (isSheetCollapsed) {
                        Text(
                            text = "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(sortedRequests) { (request, distanceKm) ->
                                val isSelected = selectedRequestId == request.id
                                MapRequestCard(
                                    request = request,
                                    isSelected = isSelected,
                                    distanceKm = distanceKm,
                                    onClick = { selectedRequestId = request.id },
                                    onViewDetails = { onRequestSelected(request.id) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { mapView },
                    update = { view ->
                        view.overlays.removeAll { it is Marker }

                        mapState.collectorLocation?.let { location ->
                            val marker = Marker(view).apply {
                                position = GeoPoint(location.latitude, location.longitude)
                                title = "Your Location"
                                icon = collectorMarkerIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            view.overlays.add(marker)
                        }

                        mapState.pendingMarkers.forEach { markerInfo ->
                            val marker = Marker(view).apply {
                                position = GeoPoint(markerInfo.latitude, markerInfo.longitude)
                                title = markerInfo.address.ifBlank { "Request #${markerInfo.requestId.take(6)}" }
                                val details = buildList {
                                    add("Status: ${markerInfo.status}")
                                    markerInfo.distanceKm?.let {
                                        add(String.format(Locale.getDefault(), "Distance: %.1f km", it))
                                    }
                                }
                                snippet = details.joinToString("\n")
                                icon = requestMarkerIcon
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                setOnMarkerClickListener { m, _ ->
                                    selectedRequestId = markerInfo.requestId
                                    view.controller.animateTo(m.position, 16.0, 500L)
                                    true
                                }
                            }
                            view.overlays.add(marker)
                        }

                        if (mapState.collectorLocation == null && mapState.pendingMarkers.isNotEmpty()) {
                            val first = mapState.pendingMarkers.first()
                            view.controller.setCenter(GeoPoint(first.latitude, first.longitude))
                        }

                        view.invalidate()
                    }
                )

                // Floating Action Button - on top of map, will be covered by bottom sheet
                FloatingActionButton(
                    onClick = {
                        shouldCenterCollector = true
                        viewModel.refreshCollectorLocation()
                    },
                    containerColor = PrimaryGreen,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        contentDescription = "My Location",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Navigation Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            CollectorBottomNavBar(
                selectedRoute = com.melodi.sampahjujur.navigation.Screen.CollectorMap.route,
                onNavigate = onNavigate
            )
        }
    }
}

private fun sortRequestsByDistance(
    requests: List<PickupRequest>,
    collectorLocation: PickupRequest.Location?
): List<Pair<PickupRequest, Double?>> {
    val enriched = requests.map { request ->
        val distance = collectorLocation?.let {
            haversineDistanceKm(
                it.latitude,
                it.longitude,
                request.pickupLocation.latitude,
                request.pickupLocation.longitude
            )
        }
        request to distance
    }

    return enriched.sortedBy { it.second ?: Double.MAX_VALUE }
}

private fun haversineDistanceKm(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val earthRadiusKm = 6371.0

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val originLat = Math.toRadians(lat1)
    val destinationLat = Math.toRadians(lat2)

    val a = sin(dLat / 2).pow(2.0) +
        sin(dLon / 2).pow(2.0) * cos(originLat) * cos(destinationLat)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    return earthRadiusKm * c
}

@Composable
private fun MapRequestCard(
    request: PickupRequest,
    isSelected: Boolean,
    distanceKm: Double? = null,
    onClick: () -> Unit,
    onViewDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        border = if (isSelected) BorderStroke(2.dp, PrimaryGreen) else null,
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = request.pickupLocation.address.ifBlank { "Pending Request" },
                fontWeight = FontWeight.SemiBold,
                maxLines = 2
            )
            distanceKm?.let {
                Text(
                    text = String.format(Locale.getDefault(), "~%.1f km away", it),
                    style = MaterialTheme.typography.bodySmall,
                    color = PrimaryGreen
                )
            }
            Text(
                text = formatDate(request.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            StatusBadge(status = request.status)
            Button(
                onClick = onViewDetails,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("View Details", color = Color.White)
            }
        }
    }
}
