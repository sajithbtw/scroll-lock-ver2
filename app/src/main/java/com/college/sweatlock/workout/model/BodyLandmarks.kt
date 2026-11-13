package com.college.sweatlock.workout.model

import kotlin.math.abs
import kotlin.math.atan2

enum class ExerciseType(val displayName: String) {
    PUSHUPS("Pushups"),
    SQUATS("Squats")
}

enum class JointType {
    LEFT_SHOULDER, RIGHT_SHOULDER,
    LEFT_ELBOW, RIGHT_ELBOW,
    LEFT_WRIST, RIGHT_WRIST,
    LEFT_HIP, RIGHT_HIP,
    LEFT_KNEE, RIGHT_KNEE,
    LEFT_ANKLE, RIGHT_ANKLE
}

data class JointPoint(
    val x: Float,
    val y: Float,
    val confidence: Float // 0.0f to 1.0f
)

data class BodyLandmarks(
    val joints: Map<JointType, JointPoint>
) {
    /**
     * Computes the interior angle (0°–180°) at vertex B formed by segments BA and BC.
     */
    fun calculateAngle(a: JointType, b: JointType, c: JointType): Float? {
        val pA = joints[a] ?: return null
        val pB = joints[b] ?: return null
        val pC = joints[c] ?: return null

        val radians = atan2(pC.y - pB.y, pC.x - pB.x) - atan2(pA.y - pB.y, pA.x - pB.x)
        var degrees = abs(Math.toDegrees(radians.toDouble())).toFloat()
        if (degrees > 180f) {
            degrees = 360f - degrees
        }
        return degrees
    }
}
