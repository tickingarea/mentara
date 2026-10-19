package com.learnagent.core

import com.learnagent.data.FrameOcr
import com.learnagent.data.LearningUnit
import com.learnagent.data.TranscriptSegment

/**
 * 单元融合：merge(transcript, frames) → TimelineItem → ContextAnalyzer → LearningUnit，
 * 并补充 visualDescription。
 */
class UnitFusion(
    private val analyzer: ContextAnalyzer = ContextAnalyzer(),
    private val visual: VisualAnalyzer = VisualAnalyzer()
) {

    suspend fun fuse(
        transcript: List<TranscriptSegment>,
        frames: List<FrameOcr>,
        videoId: String
    ): List<LearningUnit> {
        val timeline = merge(transcript, frames)
        val units = analyzer.analyze(timeline, videoId)
        return units.map { unit ->
            if (unit.screenText.isNotBlank() || unit.framePath.isNotBlank()) {
                unit.copy(visualDescription = visual.describe(unit.framePath, unit.screenText))
            } else {
                unit
            }
        }
    }
}
