package com.learnagent.ui

/**
 * UI 文案主表（English，默认）。
 * 中文全部在独立汉化包 StringsZh.kt（设置页可切换，见各端 Language 包装）。
 * 占位符 {1} {2} 由各端 t() 填充。
 */
object Strings {
    fun get(key: String, lang: String): String {
        val table = if (lang == "zh") zh else en
        return table[key] ?: en[key] ?: key
    }

    val en: Map<String, String> = mapOf(
        // ---- 状态 ----
        "status_ready" to "Ready",
        "status_imported" to "Imported: {1}",
        "status_transcribing" to "Transcribing...",
        "status_ocr" to "Extracting frames (OCR)...",
        "status_debate_running" to "Debate in progress...",
        "status_debate_done" to "Debate done · agreement {1}%",
        "status_debate_failed" to "Debate failed: {1}",
        "status_debate_cancelled" to "Debate cancelled",
        "status_saved" to "Settings saved",
        "status_need_content" to "Transcribe or extract frames first",
        "status_located" to "Located at {1}",
        "status_refreshed" to "Library refreshed",
        "status_review" to "Review: {1}",
        "status_cancelled" to "Cancelled",
        "banner_stale" to "You haven't reviewed \"{1}\" for {2} days",
        "notice_rest" to "Time to rest",

        // ---- 按钮 ----
        "btn_review_now" to "Review now",
        "btn_transcribe" to "Transcribe",
        "btn_ocr" to "Extract Frames",
        "btn_deep_analysis" to "Deep Analysis",
        "btn_cancel_debate" to "Cancel Debate",
        "btn_expand" to "Show Source",
        "btn_collapse" to "Hide Source",
        "btn_note" to "Record Experience",
        "btn_noted" to "Experience Recorded",
        "btn_save" to "Save",
        "btn_cancel" to "Cancel",
        "btn_open" to "Open",
        "btn_refresh" to "Refresh Library",
        "btn_import" to "IMPORT  Import Video",
        "btn_send" to "Send",
        "btn_settings" to "Settings",
        "btn_close" to "Close",
        "btn_retest" to "Run Storage Test",

        // ---- 搜索 ----
        "search_placeholder" to "Search video content...",
        "search_hint" to "Type to filter in real time (Esc closes)",
        "search_no_results" to "No match",
        "home_no_match" to "No videos match \"{1}\"",

        // ---- 首页 ----
        "home_welcome" to "Welcome back — {1} videos",
        "home_sub" to "Select one to begin, or grow the nebula with a new import",
        "home_empty_title" to "Begin Your Journey",
        "home_empty_sub" to "Import your first video and let knowledge start to grow",
        "home_indexed" to "{1} · {2} segments indexed",
        "home_not_indexed" to "Not indexed",
        "home_selected_hint" to "Selected",
        "home_open_hint" to "Tap to open",

        // ---- 设置 ----
        "settings_title" to "Settings",
        "settings_api_key" to "NVIDIA API Key",
        "settings_api_key_label" to "API Key (leave empty for local retrieval)",
        "settings_model" to "Model",
        "settings_model_label" to "Model id",
        "settings_debate_models" to "Debate models (leave empty for NIM defaults: Kimi K3 / DeepSeek V4.1 / GLM 5.3)",
        "settings_model_a" to "Proposal model A (Kimi K3 seat)",
        "settings_model_b" to "Proposal model B (DeepSeek V4.1 seat)",
        "settings_model_c" to "Proposal model C (GLM 5.3 seat)",
        "settings_theme" to "Theme",
        "theme_system" to "System",
        "theme_light" to "Light",
        "theme_dark" to "Dark",
        "settings_results_dir" to "Results directory",
        "settings_sound" to "Sound feedback",
        "settings_sound_desc" to "Light cues on import / Q&A / decisions",
        "settings_language" to "Language",
        "lang_en" to "English",
        "lang_zh" to "中文",

        // ---- 问答 ----
        "qa_title" to "Q&A",
        "qa_offline" to "Local retrieval",
        "qa_nim" to "NVIDIA NIM",
        "qa_placeholder" to "Ask a question...",
        "qa_thinking" to "Thinking...",
        "qa_empty_title" to "Nothing here yet",
        "qa_empty_hint" to "Try asking:",
        "qa_sug_1" to "What is this video about?",
        "qa_sug_2" to "What is gradient descent?",
        "qa_sug_3" to "What are the key takeaways?",

        // ---- 辩论 ----
        "debate_title" to "Three-Model Debate",
        "debate_offline" to "Offline · local heuristics",
        "debate_phase_1" to "Round 1 · Proposals",
        "debate_phase_2" to "Round 2 · Cross critique",
        "debate_phase_3" to "Round 3 · Verdict",
        "debate_short_1" to "Proposals",
        "debate_short_2" to "Cross critique",
        "debate_short_3" to "Verdict",
        "debate_rerun" to "Re-run Debate",
        "debate_cancel" to "Cancel",
        "debate_topic_ph" to "Enter debate topic...",
        "debate_topic_default" to "Does the core argument of \"{1}\" hold?",
        "debate_waiting_title" to "Two camps await your question in the void",
        "debate_waiting_sub" to "Edit the topic and press Re-run, or use Deep Analysis in Content",
        "debate_running" to "Debating...",
        "debate_agreement" to "Agreement",
        "debate_divergent" to "Divergence detected",
        "debate_minority" to "Minority Report",
        "debate_expand" to "expand +",
        "debate_collapse" to "collapse -",
        "debate_local_a" to "Kimi K3 (local)",
        "debate_local_b" to "DeepSeek V4.1 (local)",
        "debate_local_c" to "GLM 5.3 (local)",
        "debate_nim_a" to "Kimi K3",
        "debate_nim_b" to "DeepSeek V4.1",
        "debate_nim_c" to "GLM 5.3",

        // ---- 内容 ----
        "focus_back" to "Back",
        "content_no_results" to "No transcript / OCR results yet",
        "content_no_results_hint" to "Use Transcribe or Extract Frames above to generate",
        "content_source_missing" to "Source file missing — re-import needed",
        "content_need_content" to "Transcribe or extract frames first, then run Deep Analysis",
        "card_no_content" to "(no content)",
        "card_speech" to "Speech: {1}",
        "card_screen" to "Screen: {1}",
        "card_frame" to "Key frame: {1}",
        "card_intuition" to "INTUITION",
        "card_usage" to "USAGE",
        "card_pitfall" to "PITFALL",
        "chip_speech" to "Speech",
        "chip_screen" to "Screen",
        "file_dialog_title" to "Select a video file",

        // ---- 导航 / 标签（Android） ----
        "nav_library" to "Library",
        "nav_focus" to "Focus",
        "nav_chat" to "Q&A",
        "nav_settings" to "Settings",
        "bar_tagline" to "Mentara",
        "status_counts" to "{1} videos · {2} units",
        "status_offline" to "OFFLINE",
        "qa_unselected" to "No video selected",
        "qa_empty_retrieval" to "No content to retrieve yet",
        "qa_ready" to "Local retrieval ready",
        "qa_ask_hint" to "Ask something...",
        "focus_pick_hint" to "Pick a video in Library",
        "focus_pick_sub" to "Select a video to start focusing",
        "library_sync_hint" to "Import your first video, or sync the desktop library via Syncthing (see SYNC.md)",
        "tools_whisper" to "Whisper Transcribe Demo",
        "tools_storage" to "Storage Test",
        "settings_dir_note" to "filesDir/results (app private)",
        "test_pass" to "PASS: all checks passed!",
        "test_fail" to "FAIL: {1}",

        "status_transcribe_failed" to "Transcribe failed: {1}",
        "status_ocr_failed" to "Extract frames failed: {1}",
        "card_pending" to "Pending LearningUnit integration",

        // ---- 通用 ----
        "misc_none" to "None"
    )
}
