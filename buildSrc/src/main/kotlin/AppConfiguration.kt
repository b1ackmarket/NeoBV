import java.io.File

object AppConfiguration {
    const val appId = "dev.aaa1115910.bv"
    const val applicationId = "dev.b1ackmarket.bv"
    const val compileSdk = 36
    const val minSdk = 21
    const val targetSdk = 36
    const val firebaseProjectId = "neo-bv"
    private val rootDir: File by lazy {
        generateSequence(File(System.getProperty("user.dir")).canonicalFile) { it.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: File(System.getProperty("user.dir")).canonicalFile
    }
    private val versionTagRegex = Regex("""^v(\d+\.\d+\.\d+(?:\.\d+)?)$""")
    private val baseVersion: String by lazy {
        val githubRefName = System.getenv("GITHUB_REF_NAME")
        val githubRefType = System.getenv("GITHUB_REF_TYPE")
        val ciTag = githubRefName
            ?.takeIf { githubRefType == "tag" }
            ?.takeIf { versionTagRegex.matches(it) }
        val versionTag = ciTag ?: git("describe", "--tags", "--abbrev=0", "--match", "v[0-9]*", "HEAD")

        versionTagRegex.matchEntire(versionTag)?.groupValues?.get(1)
            ?: error("Invalid version tag: $versionTag")
    }

    val versionName: String by lazy {
        buildString {
            append(baseVersion)
            append(".r")
            append(versionCode)
            append(".")
            append(git("rev-parse", "--short=8", "HEAD"))
            if (isGitDirty()) append(".dirty")
        }
    }
    val versionCode: Int by lazy { git("rev-list", "--count", "HEAD").toInt() }
    const val libVLCVersion = "3.0.18"
    var googleServicesAvailable = true

    init {
        initConfigurations()
    }

    private fun initConfigurations() {
        val googleServicesJsonFile = File(rootDir, "app/google-services.json")
        googleServicesAvailable = googleServicesJsonFile.exists() && hasValidGoogleServicesConfig(googleServicesJsonFile)
    }

    private fun hasValidGoogleServicesConfig(googleServicesJsonFile: File): Boolean {
        val content = googleServicesJsonFile.readText()
        val projectId = Regex(""""project_id"\s*:\s*"([^"]+)"""")
            .find(content)
            ?.groupValues
            ?.get(1)
        val packageNames = Regex(""""package_name"\s*:\s*"([^"]+)"""")
            .findAll(content)
            .map { it.groupValues[1] }
            .toSet()
        return projectId == firebaseProjectId &&
            packageNames.contains(applicationId) &&
            packageNames.contains("$applicationId.r8test") &&
            packageNames.contains("$applicationId.debug")
    }

    private fun isGitDirty(): Boolean {
        val process = ProcessBuilder("git", "-C", rootDir.absolutePath, "diff", "--quiet", "HEAD", "--")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        return when (val exitCode = process.waitFor()) {
            0 -> false
            1 -> true
            else -> error("git diff --quiet HEAD -- failed with exit code $exitCode: $output")
        }
    }

    private fun git(vararg args: String): String {
        val process = ProcessBuilder(listOf("git", "-C", rootDir.absolutePath) + args)
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            error("git ${args.joinToString(" ")} failed with exit code $exitCode: $output")
        }
        return output
    }
}
