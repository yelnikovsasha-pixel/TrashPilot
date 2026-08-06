package com.trashpilot.app.core.trashdna

object TrendCalculator {
    fun storage(previous: TrashDnaSessionEntity, current: TrashDnaSessionEntity) =
        StorageTrend(current.usedStorageBytes - previous.usedStorageBytes)

    fun categories(previous: TrashDnaSessionEntity, current: TrashDnaSessionEntity): List<CategoryTrend> =
        TrashDnaCategory.entries.map { category ->
            CategoryTrend(category, current.bytes(category) - previous.bytes(category))
        }

    fun fastestGrowing(previous: TrashDnaSessionEntity, current: TrashDnaSessionEntity): CategoryTrend? =
        categories(previous, current).filter { it.changeBytes > 0 }.maxByOrNull { it.changeBytes }

    internal fun TrashDnaSessionEntity.bytes(category: TrashDnaCategory): Long = when (category) {
        TrashDnaCategory.MESSENGER_MEDIA -> messengerMediaBytes
        TrashDnaCategory.SCREENSHOTS -> screenshotBytes
        TrashDnaCategory.DOWNLOADS -> downloadBytes
        TrashDnaCategory.LARGE_FILES -> largeFileBytes
        TrashDnaCategory.HIDDEN_FILES -> hiddenFileBytes
        TrashDnaCategory.CACHE -> cacheBytes
        TrashDnaCategory.IMAGES -> imageBytes
        TrashDnaCategory.VIDEOS -> videoBytes
        TrashDnaCategory.AUDIO -> audioBytes
        TrashDnaCategory.DOCUMENTS -> documentBytes
    }
}
