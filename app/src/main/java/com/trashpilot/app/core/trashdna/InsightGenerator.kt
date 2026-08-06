package com.trashpilot.app.core.trashdna

object InsightGenerator {
    fun generate(
        previous: TrashDnaSessionEntity,
        latest: TrashDnaSessionEntity,
        storageTrend: StorageTrend,
        fastest: CategoryTrend?
    ): TrashDnaInsight = when {
        fastest?.category == TrashDnaCategory.MESSENGER_MEDIA -> TrashDnaInsight.MESSENGER_GROWTH
        fastest?.category == TrashDnaCategory.DOWNLOADS -> TrashDnaInsight.DOWNLOADS_GROWTH
        fastest?.category == TrashDnaCategory.SCREENSHOTS -> TrashDnaInsight.SCREENSHOTS_GROWTH
        latest.largeVideoBytes > previous.largeVideoBytes &&
            latest.largeVideoBytes - previous.largeVideoBytes >= (fastest?.changeBytes ?: 0) ->
            TrashDnaInsight.LARGE_VIDEOS_DOMINATE
        storageTrend.changeBytes < 0 -> TrashDnaInsight.STORAGE_REDUCED
        fastest != null -> TrashDnaInsight.CATEGORY_GROWTH
        else -> TrashDnaInsight.STORAGE_STABLE
    }
}
