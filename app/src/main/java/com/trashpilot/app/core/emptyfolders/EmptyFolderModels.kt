package com.trashpilot.app.core.emptyfolders

enum class EmptyFolderSort { NAME, NEWEST, OLDEST, PARENT }

data class EmptyFolderItem(
    val uri: String,
    val name: String,
    val parentName: String,
    val relativePath: String,
    val lastModifiedMillis: Long,
    val canDelete: Boolean
)

data class FolderObservation(
    val id: String,
    val parentId: String?,
    val hasDirectFile: Boolean,
    val readable: Boolean,
    val protected: Boolean = false
)

enum class FolderVerification { EMPTY, CONTENT, UNKNOWN, PROTECTED }

data class EmptyFolderDeletionResult(
    val deleted: List<EmptyFolderItem>,
    val failed: List<EmptyFolderItem>
)

fun verifyFolders(observations: List<FolderObservation>, rootId: String): Map<String, FolderVerification> {
    val byId = observations.associateBy(FolderObservation::id)
    val children = observations.filter { it.parentId != null }.groupBy { it.parentId }
    val memo = mutableMapOf<String, FolderVerification>()
    val active = mutableSetOf<String>()
    fun verify(id: String): FolderVerification {
        memo[id]?.let { return it }
        val node = byId[id] ?: return FolderVerification.UNKNOWN
        if (!active.add(id)) return FolderVerification.UNKNOWN
        val result = when {
            node.id != rootId && node.parentId?.let { it !in byId } == true -> FolderVerification.UNKNOWN
            node.protected -> FolderVerification.PROTECTED
            !node.readable -> FolderVerification.UNKNOWN
            node.hasDirectFile -> FolderVerification.CONTENT
            else -> {
                val childStates = children[id].orEmpty().map { verify(it.id) }
                when {
                    childStates.any { it == FolderVerification.CONTENT } -> FolderVerification.CONTENT
                    childStates.any { it == FolderVerification.UNKNOWN || it == FolderVerification.PROTECTED } -> FolderVerification.UNKNOWN
                    else -> FolderVerification.EMPTY
                }
            }
        }
        active.remove(id)
        memo[id] = result
        return result
    }
    observations.forEach { verify(it.id) }
    memo[rootId] = memo[rootId] ?: FolderVerification.UNKNOWN
    return memo
}

fun topLevelVerifiedEmptyIds(
    observations: List<FolderObservation>,
    rootId: String
): Set<String> {
    val states = verifyFolders(observations, rootId)
    return observations.asSequence()
        .filter { it.id != rootId && states[it.id] == FolderVerification.EMPTY }
        .filter { it.parentId == rootId || states[it.parentId] != FolderVerification.EMPTY }
        .map(FolderObservation::id)
        .toSet()
}

fun isProtectedEmptyFolderPath(documentId: String, selectedRootId: String): Boolean {
    val path = documentId.substringAfter(':', documentId).replace('\\', '/').trim('/')
    val rootPath = selectedRootId.substringAfter(':', selectedRootId).replace('\\', '/').trim('/')
    if (path.isBlank()) return true
    val segments = path.split('/').filter(String::isNotBlank)
    if (segments.any { it.equals("com.trashpilot.app", true) }) return true
    if (segments.firstOrNull()?.let { it.equals("data", true) || it.equals("system", true) } == true) return true
    val relative = if (rootPath.isBlank()) segments else path.removePrefix(rootPath).trim('/').split('/').filter(String::isNotBlank)
    if (relative.size != 1) return false
    return relative.single().lowercase() in REQUIRED_ROOT_DIRECTORIES
}

fun List<EmptyFolderItem>.emptyFoldersView(query: String, sort: EmptyFolderSort): List<EmptyFolderItem> {
    val comparator = when (sort) {
        EmptyFolderSort.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { item: EmptyFolderItem -> item.name }
        EmptyFolderSort.NEWEST -> compareByDescending { it.lastModifiedMillis }
        EmptyFolderSort.OLDEST -> compareBy { it.lastModifiedMillis.takeIf { value -> value > 0 } ?: Long.MAX_VALUE }
        EmptyFolderSort.PARENT -> compareBy(String.CASE_INSENSITIVE_ORDER) { item: EmptyFolderItem -> item.parentName }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    }
    return asSequence()
        .filter { query.isBlank() || it.name.contains(query, true) || it.parentName.contains(query, true) }
        .sortedWith(comparator.thenBy { it.uri })
        .toList()
}

fun accountEmptyFolderDeletion(requested: List<EmptyFolderItem>, deletedUris: Set<String>) =
    EmptyFolderDeletionResult(
        deleted = requested.filter { it.uri in deletedUris },
        failed = requested.filterNot { it.uri in deletedUris }
    )

fun toggleEmptyFolderSelection(selected: Set<String>, folder: EmptyFolderItem): Set<String> = when {
    !folder.canDelete -> selected
    folder.uri in selected -> selected - folder.uri
    else -> selected + folder.uri
}

fun selectAllDeletableFolders(selected: Set<String>, visible: List<EmptyFolderItem>): Set<String> =
    selected + visible.asSequence().filter(EmptyFolderItem::canDelete).map(EmptyFolderItem::uri)

private val REQUIRED_ROOT_DIRECTORIES = setOf(
    "android", "alarms", "dcim", "documents", "download", "downloads", "movies", "music",
    "notifications", "pictures", "podcasts", "ringtones"
)
