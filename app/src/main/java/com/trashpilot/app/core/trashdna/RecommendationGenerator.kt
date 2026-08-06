package com.trashpilot.app.core.trashdna

object RecommendationGenerator {
    fun generate(insight: TrashDnaInsight): TrashDnaRecommendation = when (insight) {
        TrashDnaInsight.MESSENGER_GROWTH -> TrashDnaRecommendation.REVIEW_MESSENGER_MEDIA
        TrashDnaInsight.DOWNLOADS_GROWTH -> TrashDnaRecommendation.REMOVE_OLD_DOWNLOADS
        TrashDnaInsight.SCREENSHOTS_GROWTH -> TrashDnaRecommendation.CHECK_SCREENSHOTS
        TrashDnaInsight.LARGE_VIDEOS_DOMINATE -> TrashDnaRecommendation.REVIEW_LARGE_VIDEOS
        TrashDnaInsight.CATEGORY_GROWTH -> TrashDnaRecommendation.REVIEW_FASTEST_CATEGORY
        TrashDnaInsight.STORAGE_REDUCED, TrashDnaInsight.STORAGE_STABLE ->
            TrashDnaRecommendation.KEEP_CURRENT_HABITS
    }
}
