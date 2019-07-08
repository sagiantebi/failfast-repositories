package com.sagiantebi.failfastrepos

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.IOException
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * Gradle plugin to ease the usage of a proxy maven repository with fallback to regular maven repositories when the proxy is inaccessible.<br/>
 * The naming comes from the need to fail (read: do not add the proxy repository) fast so we won't bottleneck the build too much.
 */
class FailFastRepositoriesPlugin : Plugin<Project> {

    var logger: Logger? = null;

    override fun apply(target: Project) {
        val extension =
            target.extensions.create("failfastRepositories", FailFastRepositoriesExtension::class.java, target)
        this.logger = target.logger
        //because we want to intervene in the configuration stage, we have no proper way of doing this other than waiting for eval.
        target.afterEvaluate {
            logger?.info("applying FailFastRepositoriesPlugin for project ${target.path} with extension - ${extension.repos}")
            val resolvedRepos: ArrayList<Repo> = ArrayList()
            val resolvedLocalRepos: ArrayList<Repo> = ArrayList()
            do {
                var running: Boolean = true
                val priorityRepo: PriorityRepo? = extension.repos.poll()
                logger?.debug("checking repo ${priorityRepo}")

                priorityRepo?.repositories?.forEach { repo ->
                    if (repo?.isLocal() == true) {
                        logger?.debug("adding ${repo} as a local repository")
                        resolvedLocalRepos.add(repo)
                    } else if (running && attemptConnection(
                            repo.url,
                            priorityRepo.connTimeoutUnit ?: TimeUnit.MILLISECONDS,
                            priorityRepo.connTimeout ?: 500
                        )
                    ) {
                        logger?.debug("adding ${repo} as a repository")
                        resolvedRepos.add(repo)
                        if (priorityRepo.exclusive) {
                            logger?.debug("${repo} is marked as exclusive, breaking the control flow")
                            running = false
                        }
                    } else if (running) {
                        logger?.info("filtered ${repo} - connection did not succeed")
                    }
                }
            } while (priorityRepo != null && running)
            resolvedRepos.forEach { repo ->
                logger?.info("Adding ${repo} to the list of maven repositories")
                if (extension.applyToAllProjects) {
                    target.allprojects
                } else {
                    Collections.singleton(target)
                }.forEach { someProject ->
                    someProject.repositories.apply {
                        maven {
                            it.setUrl(repo.url)
                            if (repo.userName != null || repo.password != null) {
                                it.credentials {
                                    it.username = repo.userName
                                    it.password = repo.password
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Attempts to connect the supplied url in the given time <br/>
     * This doesn't require any authentication methods as we check only the socket connectivity without actually passing through any data.
     */
    private fun attemptConnection(url: String, timeoutUnit: TimeUnit, timeoutValue: Long): Boolean {
        logger?.debug("attempting connection to ${url} with a timeout of ${timeoutUnit.toMillis(timeoutValue)}ms")
        var connected = false
        val theUrl: URL? = try {
            URL(url)
        } catch (mue: MalformedURLException) {
            null
        }
        val conn = theUrl?.openConnection()
        conn?.connectTimeout = timeoutUnit.toMillis(timeoutValue).toInt()
        try {
            conn?.connect()
            connected = true
        } catch (ste: SocketTimeoutException) {
        } catch (ioe: IOException) {
        }
        return connected
    }

}