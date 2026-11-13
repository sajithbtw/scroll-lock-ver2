package com.college.sweatlock

import com.college.sweatlock.workout.counter.RepCounter
import com.college.sweatlock.workout.counter.RepPhase
import com.college.sweatlock.workout.model.*
import org.junit.Assert.assertEquals
import org.junit.Test

class RepCounterTest {

    private fun buildPushupPose(wristY: Float): BodyLandmarks = BodyLandmarks(
        mapOf(
            JointType.LEFT_SHOULDER to JointPoint(0f, 0f, 0.95f),
            JointType.LEFT_ELBOW to JointPoint(0f, 100f, 0.95f),
            JointType.LEFT_WRIST to JointPoint(wristY, 100f, 0.95f), // 90° when wristY = 100f
            JointType.LEFT_HIP to JointPoint(-100f, 0f, 0.95f),
            JointType.LEFT_KNEE to JointPoint(-200f, 0f, 0.95f),
            JointType.LEFT_ANKLE to JointPoint(-300f, 0f, 0.95f)
        )
    )

    @Test
    fun countsValidPushupAndRejectsFastCheatRep() {
        val counter = RepCounter(ExerciseType.PUSHUPS)

        // Straight arm (~180°): UP
        val s1 = counter.processFrame(
            BodyLandmarks(
                buildPushupPose(0f).joints + (JointType.LEFT_WRIST to JointPoint(0f, 200f, 0.95f))
            ),
            nowMs = 1000L
        )
        assertEquals(RepPhase.UP, s1.phase)

        // Bent elbow (90°): DOWN
        val s2 = counter.processFrame(buildPushupPose(100f), nowMs = 1600L)
        assertEquals(RepPhase.DOWN, s2.phase)

        // Return UP after > 1000ms -> Valid Rep 1
        val s3 = counter.processFrame(
            BodyLandmarks(
                buildPushupPose(0f).joints + (JointType.LEFT_WRIST to JointPoint(0f, 200f, 0.95f))
            ),
            nowMs = 2200L
        )
        assertEquals(1, s3.count)

        // Cheat rep (< 1000ms after previous rep) -> Rejected
        counter.processFrame(buildPushupPose(100f), nowMs = 2400L)
        val sCheat = counter.processFrame(
            BodyLandmarks(
                buildPushupPose(0f).joints + (JointType.LEFT_WRIST to JointPoint(0f, 200f, 0.95f))
            ),
            nowMs = 2600L
        )
        assertEquals(1, sCheat.count)
    }
}
