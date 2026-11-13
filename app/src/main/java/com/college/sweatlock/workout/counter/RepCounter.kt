package com.college.sweatlock.workout.counter

import com.college.sweatlock.workout.model.BodyLandmarks
import com.college.sweatlock.workout.model.ExerciseType
import com.college.sweatlock.workout.model.JointType

enum class RepPhase { UP, DOWN }

data class RepState(
    val count: Int = 0,
    val phase: RepPhase = RepPhase.UP,
    val currentAngle: Float = 0f,
    val isFullBodyVisible: Boolean = false,
    val feedbackMessage: String = "Step back so your full body is in frame"
)

class RepCounter(
    private val exerciseType: ExerciseType,
    private val minConfidence: Float = 0.65f,
    private val minRepIntervalMs: Long = 1000L
) {
    private var count = 0
    private var phase = RepPhase.UP
    private var lastRepTimestampMs = 0L

    fun processFrame(landmarks: BodyLandmarks, nowMs: Long): RepState {
        // 1. Anti-Cheat: Ensure upper and lower body landmarks are visible
        if (!hasFullBodyInFrame(landmarks)) {
            return RepState(
                count = count,
                phase = phase,
                isFullBodyVisible = false,
                feedbackMessage = "Full body required in camera frame"
            )
        }

        // 2. Select the side (left or right) with higher landmark confidence
        val angle = when (exerciseType) {
            ExerciseType.PUSHUPS -> getBestElbowAngle(landmarks)
            ExerciseType.SQUATS -> getBestKneeAngle(landmarks)
        } ?: return RepState(
            count = count,
            phase = phase,
            isFullBodyVisible = false,
            feedbackMessage = "Position yourself sideways to camera"
        )

        val (downThreshold, upThreshold) = when (exerciseType) {
            ExerciseType.PUSHUPS -> 90f to 155f
            ExerciseType.SQUATS -> 100f to 160f
        }

        var feedback = if (phase == RepPhase.UP) "Lower down" else "Push back up!"

        // 3. State machine transitions: UP -> DOWN -> UP
        if (phase == RepPhase.UP && angle < downThreshold) {
            phase = RepPhase.DOWN
            feedback = "Good depth! Now return up"
        } else if (phase == RepPhase.DOWN && angle > upThreshold) {
            // 4. Anti-Cheat: Reject impossibly fast reps (< 1 second)
            if (nowMs - lastRepTimestampMs >= minRepIntervalMs) {
                count++
                lastRepTimestampMs = nowMs
                phase = RepPhase.UP
                feedback = "Rep $count counted!"
            } else {
                phase = RepPhase.UP
                feedback = "Too fast! Slow down for valid reps"
            }
        }

        return RepState(
            count = count,
            phase = phase,
            currentAngle = angle,
            isFullBodyVisible = true,
            feedbackMessage = feedback
        )
    }

    fun reset() {
        count = 0
        phase = RepPhase.UP
        lastRepTimestampMs = 0L
    }

    private fun hasFullBodyInFrame(landmarks: BodyLandmarks): Boolean {
        val leftChain = listOf(
            JointType.LEFT_SHOULDER, JointType.LEFT_HIP, JointType.LEFT_KNEE, JointType.LEFT_ANKLE
        ).all { (landmarks.joints[it]?.confidence ?: 0f) >= minConfidence }

        val rightChain = listOf(
            JointType.RIGHT_SHOULDER, JointType.RIGHT_HIP, JointType.RIGHT_KNEE, JointType.RIGHT_ANKLE
        ).all { (landmarks.joints[it]?.confidence ?: 0f) >= minConfidence }

        return leftChain || rightChain
    }

    private fun getBestElbowAngle(landmarks: BodyLandmarks): Float? {
        val leftConf = landmarks.joints[JointType.LEFT_ELBOW]?.confidence ?: 0f
        val rightConf = landmarks.joints[JointType.RIGHT_ELBOW]?.confidence ?: 0f
        return if (leftConf >= rightConf) {
            landmarks.calculateAngle(JointType.LEFT_SHOULDER, JointType.LEFT_ELBOW, JointType.LEFT_WRIST)
        } else {
            landmarks.calculateAngle(JointType.RIGHT_SHOULDER, JointType.RIGHT_ELBOW, JointType.RIGHT_WRIST)
        }
    }

    private fun getBestKneeAngle(landmarks: BodyLandmarks): Float? {
        val leftConf = landmarks.joints[JointType.LEFT_KNEE]?.confidence ?: 0f
        val rightConf = landmarks.joints[JointType.RIGHT_KNEE]?.confidence ?: 0f
        return if (leftConf >= rightConf) {
            landmarks.calculateAngle(JointType.LEFT_HIP, JointType.LEFT_KNEE, JointType.LEFT_ANKLE)
        } else {
            landmarks.calculateAngle(JointType.RIGHT_HIP, JointType.RIGHT_KNEE, JointType.RIGHT_ANKLE)
        }
    }
}
