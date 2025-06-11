@file:OptIn(ExperimentalUnsignedTypes::class)

package dev.arkbuilders.drop.app.ui.send

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import androidx.navigation.NavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import dev.arkbuilders.drop.SendFilesBubble
import dev.arkbuilders.drop.SendFilesConnectingEvent
import dev.arkbuilders.drop.SendFilesRequest
import dev.arkbuilders.drop.SendFilesSendingEvent
import dev.arkbuilders.drop.SendFilesSubscriber
import dev.arkbuilders.drop.SenderFile
import dev.arkbuilders.drop.SenderFileData
import dev.arkbuilders.drop.SenderProfile
import dev.arkbuilders.drop.sendFiles
import kotlinx.coroutines.delay
import java.io.InputStream
import java.util.UUID
import kotlin.random.Random

class SenderFileDataFS : SenderFileData {
    private val len: ULong
    private val inputStream: InputStream

    private var isFinished: Boolean = false

    constructor(resolver: ContentResolver, contentURI: Uri) {
        this.len = getLen(resolver, contentURI)
        this.inputStream = resolver.openInputStream(contentURI)!!
    }

    fun getLen(resolver: ContentResolver, contentURI: Uri): ULong {
        return resolver.query(contentURI, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            cursor.getLong(sizeIndex).toULong()
        } ?: 0u
    }

    override fun len(): ULong {
        return len
    }

    override fun read(): UByte? {
        if (isFinished) {
            return null
        }
        var b: UByte? = null
        try {
            val readB = inputStream.read()
            if (readB == -1) {
                isFinished = true
                inputStream.close()
            } else {
                b = readB.toUByte()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return b
    }
}

class SendFilesSubscriberImpl : SendFilesSubscriber {
    private val id: UUID = UUID.randomUUID()
    var connectingEvent: SendFilesConnectingEvent? = null
    val sendingEvents = mutableListOf<SendFilesSendingEvent>()

    override fun getId(): String {
        return this.id.toString()
    }

    override fun notifyConnecting(event: SendFilesConnectingEvent) {
        this.connectingEvent = event
    }

    override fun notifySending(event: SendFilesSendingEvent) {
        this.sendingEvents.add(event)
    }
}

data class FileState(
    val name: String,
    var sent: ULong,
    val total: ULong,
)

@Composable
fun Send(modifier: Modifier = Modifier, navController: NavController) {/* TODO:
    *   - Handle upload of files with same name.
    *   - Update file progress handling.
    */
    var isSending by remember { mutableStateOf(false) }
    var isCancelled by remember { mutableStateOf(false) }
    var visibleConfirmation by remember { mutableStateOf(false) }
    var bytesPerSecond by remember { mutableStateOf<ULong>(0u) }
    val bubble = remember { mutableStateOf<SendFilesBubble?>(null) }
    val bitmap = remember { mutableStateOf<Bitmap?>(null) }
    val request = remember { mutableStateOf<SendFilesRequest?>(null) }
    val subscriber = remember { SendFilesSubscriberImpl() }
    val fileStates = remember { mutableStateOf<List<FileState>>(emptyList()) }
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isEmpty()) {
            navController.popBackStack()
            return@rememberLauncherForActivityResult
        }
        val files = uris.map {
            SenderFile(
                name = it.lastPathSegment ?: "Unknown",
                data = SenderFileDataFS(navController.context.contentResolver, it)
            )
        }
        fileStates.value = files.map { FileState(it.name, 0u, it.data.len()) }
        request.value = SendFilesRequest(
            profile = SenderProfile(
                name = "John Doe"
            ), files = files
        )
    }
    LaunchedEffect(filePickerLauncher) {
        filePickerLauncher.launch("*/*")
    }
    LaunchedEffect(request.value) {
        if (request.value == null) return@LaunchedEffect
        bubble.value = sendFiles(request.value!!)
        bubble.value!!.subscribe(subscriber)
        val ticket = bubble.value!!.getTicket()
        val actualConfirmation = bubble.value!!.getConfirmation()
        val confirmations = createConfirmations(actualConfirmation)
        println("ticket: $ticket")
        println("confirmation: $actualConfirmation")
        bitmap.value = createQRCodeBitmap(
            data = "drop://receive?ticket=${Uri.encode(ticket)}&confirmations=${
                confirmations.map { it.toUByte() }.joinToString(",")
            }"
        )
    }
    LaunchedEffect(isCancelled) {
        if (isCancelled) {
            bubble.value?.cancel()
            navController.popBackStack()
        }
    }
    LaunchedEffect(Unit) {
        while (!isCancelled) {
            if (!isSending && subscriber.connectingEvent != null) {
                isSending = true
            }
            var sentBytes: ULong = 0u
            val sendingEvents = subscriber.sendingEvents.toList()
            subscriber.sendingEvents.clear()
            fileStates.value = fileStates.value.map { fileState ->
                val event = sendingEvents.findLast { event ->
                    event.name == fileState.name
                }
                if (event != null) {
                    sentBytes += event.sent - fileState.sent
                    fileState.sent = event.sent
                }
                fileState
            }
            bytesPerSecond = sentBytes
            delay(1000L)
        }
    }

    if (bitmap.value == null || bubble.value == null) {
        Box(
            modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (isSending) {
        Column(
            modifier = modifier
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
            Text("Sending to Jane Doe", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                fileStates.value.forEach { fileState ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(8.dp)
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
                            "${fileState.sent} of ${fileState.total} • ${
                                (fileState.total - fileState.sent).div(
                                    bytesPerSecond + 1u
                                )
                            } seconds left",
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            progress = {
                                fileState.sent.toFloat().div(fileState.total.toFloat())
                            })
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.width(8.dp))
                Text("Confirmation code", fontSize = 18.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                bubble.value!!.getConfirmation().toUInt().toString().padStart(2, '0').toCharArray()
                    .forEach { char ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            if (visibleConfirmation) {
                                Text(char.toString(), fontSize = 24.sp)
                            } else {
                                Text("·", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                visibleConfirmation = !visibleConfirmation
            }) {
                Text(if (visibleConfirmation) "Hide" else "Show")
            }
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .padding(16.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    modifier = Modifier.padding(16.dp),
                    bitmap = bitmap.value!!.asImageBitmap(),
                    contentDescription = "Send Files QR Code"
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("Waiting for connection…", fontSize = 16.sp)
        }
    }

}

private fun createConfirmations(actualConfirmation: UByte): List<UByte> {
    val confirmations = mutableListOf<UByte>()
    confirmations.add(Random.nextInt(100).toUByte())
    confirmations.add(Random.nextInt(100).toUByte())
    confirmations.add(actualConfirmation)
    confirmations.shuffle()
    return confirmations
}

private fun createQRCodeBitmap(data: String, size: Int = 920): Bitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 1)
    val matrix = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints)
    return createBitmap(size, size, Bitmap.Config.RGB_565).apply {
        for (x in 0 until size) {
            for (y in 0 until size) {
                set(x, y, if (matrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
    }
}
