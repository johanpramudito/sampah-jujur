package com.melodi.sampahjujur.ui.screens.collector

import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
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

    LaunchedEffect(selectedRequestId, mapState.pendingMarkers) {
        selectedRequestId?.let { requestId ->
            mapState.pendingMarkers.firstOrNull { it.requestId == requestId }?.let { markerInfo ->
                val point = GeoPoint(markerInfo.latitude, markerInfo.longitude)
                mapView.controller.animateTo(point, 16.0, 1000L)
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Pickup Map", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    Icon(imageVector = Icons.Default.Map, contentDescription = null, tint = PrimaryGreen)
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    shouldCenterCollector = true
                    viewModel.refreshCollectorLocation()
                },
                containerColor = PrimaryGreen
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location", tint = Color.White)
            }
        },
        bottomBar = {
            CollectorBottomNavBar(
                selectedRoute = com.melodi.sampahjujur.navigation.Screen.CollectorMap.route,
                onNavigate = onNavigate
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { mapView },
                update = { view ->
                    // Remove existing markers to avoid duplicates
                    view.overlays.removeAll { it is Marker }

                    // Collector marker
                    mapState.collectorLocation?.let { location ->
                        val marker = Marker(view).apply {
                            position = GeoPoint(location.latitude, location.longitude)
                            title = "Your Location"
                            icon = collectorMarkerIcon
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        }
                        view.overlays.add(marker)
                    }

                    // Pending request markers
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

            if (pendingRequests.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.95f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(pendingRequests) { request ->
                            val isSelected = selectedRequestId == request.id
                            val distanceKm = mapState.pendingMarkers.firstOrNull { it.requestId == request.id }?.distanceKm
                            MapRequestCard(
                                request = request,
                                isSelected = isSelected,
                                distanceKm = distanceKm,
                                onClick = {
                                    selectedRequestId = request.id
                                },
                                onViewDetails = { onRequestSelected(request.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapRequestCard(
    request: PickupRequest,
    isSelected: Boolean,
    distanceKm: Double? = null,
    onClick: () -> Unit,
    onViewDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(240.dp)
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
