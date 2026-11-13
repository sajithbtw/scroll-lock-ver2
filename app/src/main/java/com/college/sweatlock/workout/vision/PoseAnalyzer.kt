package com.college.sweatlock.workout.vision

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.college.sweatlock.workout.model.BodyLandmarks
import com.college.sweatlock.workout.model.JointPoint
import com.college.sweatlock.workout.model.JointType
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

data class PoseFrameResult(
    val landmarks: BodyLandmarks,
    val sourceWidth: Int,
    val sourceHeight: Int
)

class PoseAnalyzer(
    private val onPoseDetected: (PoseFrameResult) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Swap dimensions when rotated 90° or 270° so overlay matches sensor orientation
        val isRotated = rotationDegrees == 90 || rotationDegrees == 270
        val uprightWidth = if (isRotated) imageProxy.height else imageProxy.width
        val uprightHeight = if (isRotated) imageProxy.width else imageProxy.height

        detector.process(inputImage)
            .addOnSuccessListener { pose ->
                onPoseDetected(
                    PoseFrameResult(
                        landmarks = pose.toBodyLandmarks(),
                        sourceWidth = uprightWidth,
                        sourceHeight = uprightHeight
                    )
                )
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    fun close() {
        detector.close()
    }

    private fun Pose.toBodyLandmarks(): BodyLandmarks {
        val mapping = mapOf(
            JointType.LEFT_SHOULDER to PoseLandmark.LEFT_SHOULDER,
            JointType.RIGHT_SHOULDER to PoseLandmark.RIGHT_SHOULDER,
            JointType.LEFT_ELBOW to PoseLandmark.LEFT_ELBOW,
            JointType.RIGHT_ELBOW to PoseLandmark.RIGHT_ELBOW,
            JointType.LEFT_WRIST to PoseLandmark.LEFT_WRIST,
            JointType.RIGHT_WRIST to PoseLandmark.RIGHT_WRIST,
            JointType.LEFT_HIP to PoseLandmark.LEFT_HIP,
            JointType.RIGHT_HIP to PoseLandmark.RIGHT_HIP,
            JointType.LEFT_KNEE to PoseLandmark.LEFT_KNEE,
            JointType.RIGHT_KNEE to PoseLandmark.RIGHT_KNEE,
            JointType.LEFT_ANKLE to PoseLandmark.LEFT_ANKLE,
            JointType.RIGHT_ANKLE to PoseLandmark.RIGHT_ANKLE
        )

        val joints = mapping.mapNotNull { (jointType, mlKitType) ->
            getPoseLandmark(mlKitType)?.let { lm ->
                jointType to JointPoint(
                    x = lm.position.x,
                    y = lm.position.y,
                    confidence = lm.inFrameLikelihood
                )
            }
        }.toMap()

        return BodyLandmarks(joints)
    }
}
