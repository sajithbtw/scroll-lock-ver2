package com.college.sweatlock.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.college.sweatlock.workout.model.BodyLandmarks
import com.college.sweatlock.workout.model.JointType
import kotlin.math.max

@Composable
fun SkeletonOverlay(
    landmarks: BodyLandmarks?,
    sourceWidth: Int,
    sourceHeight: Int,
    isFrontCamera: Boolean = true,
    isValidBody: Boolean = false
) {
    if (landmarks == null || sourceWidth == 0 || sourceHeight == 0) return

    val boneColor = if (isValidBody) Color(0xFF9D7BFF) else Color(0xFFFF5E7E)
    val jointColor = if (isValidBody) Color(0xFF4ADE80) else Color(0xFFFBBF24)

    val connections = listOf(
        JointType.LEFT_SHOULDER to JointType.RIGHT_SHOULDER,
        JointType.LEFT_SHOULDER to JointType.LEFT_ELBOW,
        JointType.LEFT_ELBOW to JointType.LEFT_WRIST,
        JointType.RIGHT_SHOULDER to JointType.RIGHT_ELBOW,
        JointType.RIGHT_ELBOW to JointType.RIGHT_WRIST,
        JointType.LEFT_SHOULDER to JointType.LEFT_HIP,
        JointType.RIGHT_SHOULDER to JointType.RIGHT_HIP,
        JointType.LEFT_HIP to JointType.RIGHT_HIP,
        JointType.LEFT_HIP to JointType.LEFT_KNEE,
        JointType.LEFT_KNEE to JointType.LEFT_ANKLE,
        JointType.RIGHT_HIP to JointType.RIGHT_KNEE,
        JointType.RIGHT_KNEE to JointType.RIGHT_ANKLE
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Match CameraX PreviewView FILL_CENTER scaling
        val scale = max(size.width / sourceWidth, size.height / sourceHeight)
        val offsetX = (size.width - sourceWidth * scale) / 2f
        val offsetY = (size.height - sourceHeight * scale) / 2f

        fun mapPoint(x: Float, y: Float): Offset {
            val mappedX = if (isFrontCamera) {
                size.width - (x * scale + offsetX)
            } else {
                x * scale + offsetX
            }
            val mappedY = y * scale + offsetY
            return Offset(mappedX, mappedY)
        }

        // Draw bones
        connections.forEach { (startType, endType) ->
            val start = landmarks.joints[startType]
            val end = landmarks.joints[endType]
            if (start != null && end != null && start.confidence > 0.5f && end.confidence > 0.5f) {
                drawLine(
                    color = boneColor,
                    start = mapPoint(start.x, start.y),
                    end = mapPoint(end.x, end.y),
                    strokeWidth = 8f,
                    cap = StrokeCap.Round
                )
            }
        }

        // Draw joints
        landmarks.joints.values.filter { it.confidence > 0.5f }.forEach { joint ->
            drawCircle(
                color = jointColor,
                radius = 12f,
                center = mapPoint(joint.x, joint.y)
            )
        }
    }
}
