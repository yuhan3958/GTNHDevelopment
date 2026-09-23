package dev.gtnh.intellij.compat

import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ChangeListManager
import dev.gtnh.intellij.contribution.GtnhGitState
import git4idea.repo.GitRepositoryManager

class GitStateAdapter(private val project: Project) {
    fun read(): GtnhGitState {
        val repository = GitRepositoryManager.getInstance(project).repositories.firstOrNull()
            ?: return GtnhGitState(false, null, false, emptyList(), emptyList(), null, null, null)
        val changed = ChangeListManager.getInstance(project).affectedFiles.map { it.path }.sorted()
        val remotes = repository.remotes.map { remote ->
            val url = remote.firstUrl ?: "no URL"
            "${remote.name}: $url"
        }
        return GtnhGitState(
            available = true,
            branch = repository.currentBranchName,
            detached = repository.currentBranchName == null && repository.currentRevision != null,
            changedPaths = changed,
            remotes = remotes,
            upstream = null,
            ahead = null,
            behind = null
        )
    }
}
