
package com.example.arhideandseek

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.google.ar.core.Config
import com.google.ar.core.Frame
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.math.Position
import io.github.sceneview.node.SphereNode
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ARHideAndSeekGame() }
    }
}

private data class HiddenItem(
    val anchorNode: AnchorNode,
    val itemNode: SphereNode
)

@Composable
private fun ARHideAndSeekGame() {
    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val childNodes = rememberNodes()

    var frame by remember { mutableStateOf<Frame?>(null) }
    val items = remember { mutableStateListOf<HiddenItem>() }
    var foundCount by remember { mutableIntStateOf(0) }
    var spawning by remember { mutableStateOf(false) }

    val targetCount = 7
    val revealDistanceMeters = 1.2f
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    fun resetGame() {
        childNodes.clear()
        items.clear()
        foundCount = 0
        spawning = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { viewSize = it.size }
    ) {
        ARScene(
            modifier = Modifier.fillMaxSize(),
            engine = engine,
            modelLoader = modelLoader,
            materialLoader = materialLoader,
            planeRenderer = true,
            cameraStream = rememberARCameraStream(materialLoader),
            sessionConfiguration = { session, config ->
                config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                config.depthMode =
                    if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                        Config.DepthMode.AUTOMATIC
                    } else {
                        Config.DepthMode.DISABLED
                    }
            },
            childNodes = childNodes,
            onSessionUpdated = { _, updatedFrame ->
                frame = updatedFrame
                val pose = updatedFrame.camera.pose
                val cameraPos = Position(
                    x = pose.tx(),
                    y = pose.ty(),
                    z = pose.tz()
                )

                if (spawning && viewSize.width > 0 && viewSize.height > 0) {
                    repeat(12) {
                        if (items.size >= targetCount) return@repeat

                        val x = Random.nextInt(0, viewSize.width).toFloat()
                        val y = Random.nextInt(0, viewSize.height).toFloat()

                        val hit = updatedFrame.hitTest(x, y)
                            .firstOrNull { it.isValid(depthPoint = false, point = false) }
                            ?: return@repeat

                        val anchor = hit.createAnchorOrNull() ?: return@repeat
                        val anchorNode = AnchorNode(engine, anchor)

                        val sphere = SphereNode(
                            engine = engine,
                            radius = 0.08f,
                            materialInstance = materialLoader.createColorInstance(
                                color = Color(0xFFFFD400)
                            )
                        ).apply { isVisible = false }

                        anchorNode.addChildNode(sphere)
                        childNodes += anchorNode
                        items += HiddenItem(anchorNode, sphere)
                    }
                    if (items.size >= targetCount) spawning = false
                }

                items.forEach {
                    if (!it.itemNode.isVisible) {
                        val d = distanceMeters(cameraPos, it.itemNode.worldPosition)
                        if (d <= revealDistanceMeters) {
                            it.itemNode.isVisible = true
                            foundCount += 1
                        }
                    }
                }
            }
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Found: $foundCount / $targetCount")
            Button(onClick = { resetGame() }) {
                Text(if (spawning || items.isNotEmpty()) "Restart" else "Start")
            }
            Text(if (spawning) "Scan slowly to place hidden items..." else "Move around to find them.")
        }
    }
}

private fun distanceMeters(a: Position, b: Position): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    val dz = a.z - b.z
    return sqrt(dx * dx + dy * dy + dz * dz)
}
