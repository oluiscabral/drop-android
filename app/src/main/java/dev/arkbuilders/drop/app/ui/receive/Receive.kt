package dev.arkbuilders.drop.app.ui.receive

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import dev.arkbuilders.drop.ReceiveFilesBubble
import dev.arkbuilders.drop.ReceiveFilesConnectingEvent
import dev.arkbuilders.drop.ReceiveFilesReceivingEvent
import dev.arkbuilders.drop.ReceiveFilesRequest
import dev.arkbuilders.drop.ReceiveFilesSubscriber
import dev.arkbuilders.drop.ReceiverProfile
import dev.arkbuilders.drop.receiveFiles
import kotlinx.coroutines.delay
import java.util.UUID
import java.util.concurrent.Executors


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Receive(
    ticket: String?,
    confirmations: List<UByte>,
    onBack: () -> Unit = {},
    onReceive: (List<ReceiverChunk>) -> Unit = {},
    onScanQRCode: (Uri) -> Unit = {},
) {
    var selectedConfirmation by remember { mutableStateOf<UByte?>(null) }

    if (ticket != null && selectedConfirmation != null) {
        ReceiveFiles(ticket, selectedConfirmation!!, onBack, onReceive)
    } else if (confirmations.isEmpty()) {
        ScanQRCode(onScanQRCode)
    } else {
        SelectConfirmation(
            confirmations,
            onBack,
            onSelectConfirmation = { confirmation ->
                selectedConfirmation = confirmation
            },
        )
    }
}

class ReceiveFilesSubscriberImpl : ReceiveFilesSubscriber {
    private val id: UUID = UUID.randomUUID()
    val connectingEvent = mutableStateOf<ReceiveFilesConnectingEvent?>(null)
    val receivingEvents = mutableListOf<ReceiveFilesReceivingEvent>()

    override fun getId(): String {
        return this.id.toString()
    }

    override fun notifyConnecting(event: ReceiveFilesConnectingEvent) {
        this.connectingEvent.value = event
    }

    override fun notifyReceiving(event: ReceiveFilesReceivingEvent) {
        this.receivingEvents.add(event)
    }
}

data class FileState(
    val id: String,
    val name: String,
    var received: ULong,
    val total: ULong,
)

data class ReceiverChunk(
    val id: String,
    val name: String,
    val data: List<UByte>,
)

@Composable
fun ReceiveFiles(
    ticket: String,
    confirmation: UByte,
    onBack: () -> Unit = {},
    onReceive: (List<ReceiverChunk>) -> Unit = {}
) {
    val subscriber = remember { ReceiveFilesSubscriberImpl() }
    val request = remember {
        ReceiveFilesRequest(
            ticket, confirmation, profile = ReceiverProfile("John Doe")
        )
    }
    val fileStates = remember { mutableStateOf<List<FileState>>(emptyList()) }
    var isCancelled by remember { mutableStateOf(false) }
    var bubble by remember { mutableStateOf<ReceiveFilesBubble?>(null) }
    var bytesPerSecond by remember { mutableStateOf<ULong>(0u) }

    LaunchedEffect(request) {
        println("LaunchedEffect called with request: $request")
        try {
            bubble = receiveFiles(request)
            bubble!!.subscribe(subscriber)
            bubble!!.start()
        } catch (e: Exception) {
            isCancelled = true
            e.printStackTrace()
        }
    }
    LaunchedEffect(isCancelled) {
        println("LaunchedEffect called with isCancelled: $isCancelled")
        if (isCancelled) {
            bubble?.cancel()
            onBack()
        }
    }
    LaunchedEffect(subscriber.connectingEvent.value) {
        println("LaunchedEffect called with subscriber.connectingEvent.value: ${subscriber.connectingEvent.value}")
        if (subscriber.connectingEvent.value == null) return@LaunchedEffect
        fileStates.value = subscriber.connectingEvent.value!!.files.map {
            FileState(it.id, it.name, 0u, it.len)
        }
        var emptyReceivingEventsCounter = 0
        while (!isCancelled) {
            var receivedBytes: ULong = 0u
            val receivingEvents = subscriber.receivingEvents.toList()
            subscriber.receivingEvents.clear()
            if (receivingEvents.isEmpty()) {
                emptyReceivingEventsCounter++
            } else {
                emptyReceivingEventsCounter = 0
            }
            if (emptyReceivingEventsCounter > 10) {
                bytesPerSecond = 0u
                break;
            }
            val chunks: List<ReceiverChunk> =
                receivingEvents.groupBy { it.id }.map { (id, events) ->
                    val fileState = fileStates.value.find { it.id == id }!!
                    val accumulatedData = events.flatMap { it.data }
                    ReceiverChunk(id, fileState.name, accumulatedData)
                }
            onReceive(chunks)
            chunks.forEach { chunk ->
                fileStates.value = fileStates.value.map { fileState ->
                    if (fileState.id == chunk.id) {
                        receivedBytes += chunk.data.size.toULong()
                        fileState.received += chunk.data.size.toULong()
                    }
                    fileState
                }
            }
            bytesPerSecond = receivedBytes
            delay(1000L)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy((-12).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            )
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f))
            )
        }
        Spacer(Modifier.height(16.dp))
        Text("Wait a moment while transferring…", fontSize = 18.sp)
        Text("Receiving from Jane Doe", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()
        ) {
            fileStates.value.forEach { fileState ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(fileState.name, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${fileState.received} of ${fileState.total} • ${
                            (fileState.total - fileState.received).div(
                                bytesPerSecond + 1u
                            )
                        } seconds left", fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp), progress = {
                            fileState.received.toFloat().div(fileState.total.toFloat())
                        })
                }
            }
        }
    }
}

@Composable
fun ScanQRCode(onScanQRCode: (Uri) -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* TODO: handle denial */ }

    LaunchedEffect(Unit) {
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

    if (ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        AndroidView(factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }
        }, modifier = Modifier.fillMaxSize()) { previewView ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val previewUseCase = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val options =
                    BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build()
                val scanner = BarcodeScanning.getClient(options)
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                    .also { aa ->
                        val executor = Executors.newSingleThreadExecutor()
                        aa.setAnalyzer(executor) { proxy ->
                            processImageProxy(scanner, proxy) { qr ->
                                val data = qr.toUri()
                                onScanQRCode(data).also { proxy.close() }
                            }
                        }
                    }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, previewUseCase, analysis
                )
            }, ContextCompat.getMainExecutor(context))
        }
    } else {
        Text("Camera permission required")
    }
}

@androidx.annotation.OptIn(ExperimentalGetImage::class)
private fun processImageProxy(
    barcodeScanner: BarcodeScanner, imageProxy: ImageProxy, onQrCodeScanned: (String) -> Unit
) {
    imageProxy.image?.let { image ->
        val inputImage = InputImage.fromMediaImage(
            image, imageProxy.imageInfo.rotationDegrees
        )

        barcodeScanner.process(inputImage).addOnSuccessListener { barcodes ->
            barcodes.firstOrNull()?.rawValue?.let { qrValue ->
                if (qrValue.startsWith("drop://receive")) {
                    onQrCodeScanned(qrValue)
                }
            }
        }.addOnCompleteListener {
            imageProxy.close()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectConfirmation(
    confirmations: List<UByte>, onBack: () -> Unit, onSelectConfirmation: (UByte) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
            }, title = { Text("Back") })
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(32.dp))
            Text("Choose the confirmation Code", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Make sure code confirmation are matched", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                confirmations.forEach { confirmation ->
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF0F3F5),
                        modifier = Modifier
                            .size(64.dp)
                            .clickable {
                                onSelectConfirmation(confirmation)
                            },
                        tonalElevation = 0.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                confirmation.toULong().toString().padStart(2, '0'),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
