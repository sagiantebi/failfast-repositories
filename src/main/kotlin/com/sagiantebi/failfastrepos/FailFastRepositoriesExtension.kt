package com.sagiantebi.failfastrepos

import org.gradle.api.Project
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

open class FailFastRepositoriesExtension constructor(val project: Project) {

    var applyToAllProjects = false

    internal val repos: Queue<PriorityRepo> =
        PriorityQueue(Comparator { o1: PriorityRepo, o2: PriorityRepo -> o1.weight().ordinal - o2.weight().ordinal }.thenComparing { o1: PriorityRepo, o2: PriorityRepo -> o1.invokationCount() - o2.invokationCount() })

    private var counter = 0

    @JvmOverloads
    fun failFast(repo: Repo, connTimeout: Long = 5_00L, connTimeoutUnit: TimeUnit = TimeUnit.MILLISECONDS) =
        repos.add(SingleFailFastRepository(repo, connTimeout, connTimeoutUnit, counter++))

    fun fallback(repos: List<Repo>) = this.repos.add(MultipleRepositories(repos, counter++))

    companion object {
        @JvmOverloads
        @JvmStatic
        fun ofRepository(url: String, userName: String? = null, password: String? = null): Repo =
            Repo(url, userName, password)
    }

}

data class Repo @JvmOverloads constructor(val url: String, val userName: String? = null, val password: String? = null) {
    val resolved: URL? = try {
        URL(url)
    } catch (ignored: MalformedURLException) {
        null
    }

    fun isLocal() = resolved?.protocol?.equals("file")
}

internal enum class PriorityWeight {
    CHECK_FIRST,
    CHECK_LATER
}

internal interface PriorityRepo {
    val repositories: Stream<Repo>
    val exclusive: Boolean //for future usage. now only allows SingleFailFastRepository to receive exclusivity.
    val connTimeoutUnit: TimeUnit?
    val connTimeout: Long?
    fun invokationCount(): Int
    fun weight(): PriorityWeight
}

internal data class SingleFailFastRepository(
    val repo: Repo,
    override val connTimeout: Long = 5_00L,
    override val connTimeoutUnit: TimeUnit = TimeUnit.MILLISECONDS,
    val invokationCounter: Int = 0,
    override val exclusive: Boolean = true
) : PriorityRepo {
    override fun invokationCount(): Int {
        return invokationCounter
    }

    override fun weight(): PriorityWeight {
        return PriorityWeight.CHECK_FIRST
    }

    override val repositories: Stream<Repo>
        get() = Stream.of(repo)
}

internal data class MultipleRepositories(val urls: List<Repo>, val invokationCounter: Int = 0) : PriorityRepo {

    override val connTimeoutUnit: TimeUnit?
        get() = TimeUnit.MILLISECONDS //TODO find a better common ancestor ?

    override val connTimeout: Long?
        get() = 5_00

    override fun invokationCount(): Int {
        return invokationCounter
    }

    override fun weight(): PriorityWeight {
        return PriorityWeight.CHECK_LATER
    }

    override val repositories: Stream<Repo>
        get() = urls.stream()

    override val exclusive: Boolean
        get() = false

}