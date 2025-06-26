/*
 * BreweryX Bukkit-Plugin for an alternate brewing process
 * Copyright (C) 2024 The Brewery Team
 *
 * This file is part of BreweryX.
 *
 * BreweryX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * BreweryX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with BreweryX. If not, see <http://www.gnu.org/licenses/gpl-3.0.html>.
 */

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.papermc.hangarpublishplugin.model.Platforms
import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.tools.ant.filters.ReplaceTokens
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.Charset

plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version "8.3.5"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("com.modrinth.minotaur") version "2.8.7"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.github.0ffz.github-packages") version "1.2.1"
}

group = "com.dre.brewery"
version = "3.4.10"
val langVersion: Int = 21
val encoding: String = "UTF-8"

repositories {
    mavenCentral()
    maven("https://repo.jsinco.dev/releases") // UniversalScheduler
    maven("https://jitpack.io") // GriefPrevention, SlimeFun, PlaceholderAPI
    maven("https://repo.md-5.net/content/groups/public/") // Bungee
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven("https://nexus.hc.to/content/repositories/pub_releases") // Vault
    maven("https://maven.enginehub.org/repo/") // WorldEdit, WorldGuard
    maven("https://ci.ender.zone/plugin/repository/everything/") // LWC Extended
    maven("https://repo.minebench.de/") // ChestShop
    maven("https://repo.codemc.org/repository/maven-public/") // BlockLocker
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/") // MythicLib (MMOItems)
    maven("https://repo.projectshard.dev/repository/releases/") // Shopkeepers
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
    maven("https://repo.glaremasters.me/repository/towny/") // Towny
    maven("https://repo.oraxen.com/releases") // Oraxen
    maven("https://storehouse.okaeri.eu/repository/maven-public/") // Okaeri Config
    maven("https://repo.papermc.io/repository/maven-public/") // PaperLib
    maven { githubPackage("apdevteam/movecraft")(this) } // Movecraft
}

dependencies {
    // Spigot
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson") // Implemented manually
    }
    // Paper Lib, performance improvements on Paper-based servers and async teleporting on Folia
    implementation("io.papermc:paperlib:1.0.8")

    // Implemented manually mainly due to older server versions implementing versions of GSON
    // which don't support records.
    implementation("com.google.code.gson:gson:2.11.0")
    // For proper scheduling between Bukkit-Folia like servers, https://github.com/Anon8281/UniversalScheduler
    implementation("com.github.Anon8281:UniversalScheduler:0.1.3-dev")
    // Nice annotations, I prefer these to Lombok's, https://www.jetbrains.com/help/idea/annotating-source-code.html
    compileOnly("org.jetbrains:annotations:26.0.1")
    // MongoDB & log4j to suppress MongoDB's logger
    implementation("org.mongodb:mongodb-driver-sync:5.3.0-beta0")
    compileOnly("org.apache.logging.log4j:log4j-core:2.23.1")
    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    // Okaeri configuration
    implementation("eu.okaeri:okaeri-configs-yaml-snakeyaml:5.0.5") {
        exclude("org.yaml", "snakeyaml")
    }
    constraints {
        implementation("org.yaml:snakeyaml") {
            version {
                require("2.3")
                reject("1.33")
            }
        }
    }

    // Plugin Compatability
    compileOnly("net.milkbowl.vault:VaultAPI:1.6")
    compileOnly("com.sk89q:worldguard:6.1") // https://dev.bukkit.org/projects/worldedit/files
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.7")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT") // https://dev.bukkit.org/projects/worldedit/files
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.0-SNAPSHOT") // https://dev.bukkit.org/projects/worldguard/files
    compileOnly("com.griefcraft:lwc:2.3.2") { // https://www.spigotmc.org/resources/lwc-extended.69551/history
        exclude("com.google")
    }
    compileOnly("com.github.TechFortress:GriefPrevention:16.18") // https://www.spigotmc.org/resources/griefprevention.1884/history
    compileOnly("de.diddiz:logblock:1.16.5.1") // https://www.spigotmc.org/resources/logblock.67333/history
    compileOnly("com.github.Slimefun:Slimefun4:RC-35") // https://github.com/Slimefun/Slimefun4/releases
    compileOnly("io.lumine:MythicLib-dist:1.6-SNAPSHOT") // https://www.spigotmc.org/resources/mythiclib.90306/history
    compileOnly("com.acrobot.chestshop:chestshop:3.12.2") // https://github.com/ChestShop-authors/ChestShop-3/releases
    compileOnly("com.palmergames.bukkit.towny:towny:0.100.3.0") // https://www.spigotmc.org/resources/towny-advanced.72694/history
    compileOnly("com.github.Angeschossen:LandsAPI:7.11.10") // https://www.spigotmc.org/resources/lands.53313/history
    compileOnly("com.nisovin.shopkeepers:ShopkeepersAPI:2.18.0") // https://www.spigotmc.org/resources/shopkeepers.80756/history
    compileOnly("nl.rutgerkok:blocklocker:1.10.4") // https://www.spigotmc.org/resources/blocklocker.3268/history
    compileOnly("me.clip:placeholderapi:2.11.5") // https://www.spigotmc.org/resources/placeholderapi.6245/history
    compileOnly("io.th0rgal:oraxen:1.163.0") // https://www.spigotmc.org/resources/%E2%98%84%EF%B8%8F-oraxen-custom-items-blocks-emotes-furniture-resourcepack-and-gui-1-18-1-21-3.72448/
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.1") // https://www.spigotmc.org/resources/%E2%9C%A8itemsadder%E2%AD%90emotes-mobs-items-armors-hud-gui-emojis-blocks-wings-hats-liquids.73355/updates
    compileOnly("net.countercraft:movecraft:+") //https://hangar.papermc.io/Airship-Pirates/Movecraft


    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}



tasks {

    build {
        dependsOn(shadowJar)
    }
    jar {
        enabled = false // Shadow produces our jar files
    }
    withType<JavaCompile>().configureEach {
        options.encoding = encoding
    }
    test {
        useJUnitPlatform()
    }

    processResources {
        outputs.upToDateWhen { false }
        filter<ReplaceTokens>(mapOf(
            "tokens" to mapOf("version" to "${project.version};${getGitBranch()}"),
            "beginToken" to "\${",
            "endToken" to "}"
        )).filteringCharset = encoding
    }

    shadowJar {
        val pack = "com.dre.brewery.depend"
		relocate("com.google.gson", "$pack.google.gson")
        relocate("com.google.errorprone", "$pack.google.errorprone")
        relocate("com.github.Anon8281.universalScheduler", "$pack.universalScheduler")
        relocate("eu.okaeri", "$pack.okaeri")
        relocate("com.mongodb", "$pack.mongodb")
        relocate("org.bson", "$pack.bson")
        relocate("io.papermc.lib", "$pack.paperlib")

        archiveClassifier.set("")
    }

    register("publishRelease") {
        println("Publishing a new release to: modrinth, hangar, and maven")
        dependsOn(modrinth)
        finalizedBy("publishPluginPublicationToHangar")
        finalizedBy(publish)

        doLast {
            // Much rather use a task in Gradle than a GitHub action for this,
            // but, may want to look into finding a small plugin for this since BreweryX has
            // a variety of addons that would also need this code copied into them.
            val webhook = DiscordWebhook(System.getenv("DISCORD_WEBHOOK") ?: return@doLast)
            webhook.message = "@everyone"
            webhook.embedTitle = "BreweryX - v${project.version}"
            webhook.embedDescription = readChangeLog()
            webhook.send()
        }
    }

    runServer {
        minecraftVersion("1.21.4")
    }

    register("publishToDiscord") {
        val webhook = DiscordWebhook(System.getenv("DISCORD_WEBHOOK"))
        webhook.message = "@everyone"
        webhook.embedTitle = "BreweryX - v${project.version}"
        webhook.embedDescription = readChangeLog()
        webhook.send()
    }
}

tasks.withType(xyz.jpenilla.runtask.task.AbstractRun::class) {
    javaLauncher = javaToolchains.launcherFor {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(21)
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(langVersion)
    val b = System.getProperty("sources")
    if (b != null && b.toBoolean()) {
        withSourcesJar()
    }
}


publishing {
    val repoUrl = System.getenv("REPO_URL") ?: "https://repo.jsinco.dev/releases"
    val user = System.getenv("REPO_USERNAME")
    val pass = System.getenv("REPO_SECRET")


    repositories {
        if (user == null || pass == null) {
            return@repositories
        }
        maven {
            url = uri(repoUrl)
            credentials(PasswordCredentials::class) {
                username = user
                password = pass
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        if (user == null || pass == null) {
            return@publications
        }
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            artifact(tasks.shadowJar.get().archiveFile) {
                builtBy(tasks.shadowJar)
            }
        }
    }
}


modrinth {
    token.set(System.getenv("MODRINTH_TOKEN") ?: run {
        return@modrinth
    })
    projectId.set(project.name) // This can be the project ID or the slug. Either will work!
    versionNumber.set(project.version.toString())
    versionType.set("release") // This is the default -- can also be `beta` or `alpha`
    uploadFile.set(tasks.shadowJar)
    loaders.addAll("bukkit", "spigot", "paper", "purpur", "folia")
    gameVersions.addAll("1.13.2", "1.14.4", "1.15.2", "1.16.5", "1.17.1", "1.18.2", "1.19.4",
        "1.20.4", "1.21", "1.21.1", "1.21.2", "1.21.3", "1.21.4")
    changelog.set(readChangeLog())
}


hangarPublish {
    publications.register("plugin") {
        version.set(project.version.toString())
        channel.set("Release")
        id.set(project.name)
        apiKey.set(System.getenv("HANGAR_TOKEN") ?: return@register)
        changelog.set(readChangeLog())
        platforms {
            register(Platforms.PAPER) {
                // TODO: Ask in paper discord
                //url.set("https://modrinth.com/plugin/breweryx/versions")
                jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                platformVersions.set(listOf("1.13.x", "1.14.x", "1.15.x", "1.16.x", "1.17.x", "1.18.x", "1.19.x", "1.20.x", "1.21.x"))
            }
        }
    }
}



fun getGitBranch(): String = ByteArrayOutputStream().use { stream ->
    var branch = "none"
    project.exec {
        commandLine = listOf("git", "rev-parse", "--abbrev-ref", "HEAD")
        standardOutput = stream
    }
    if (stream.size() > 0) branch = stream.toString(Charset.defaultCharset().name()).trim()
    return branch
}

fun readChangeLog(): String {
    val text: String = System.getenv("CHANGELOG") ?: file("CHANGELOG.md").run {
        if (exists()) readText() else "No Changelog found."
    }
    return text.replace("\${version}", project.version.toString())
}


class DiscordWebhook(
    val webhookUrl: String,
    var defaultThumbnail: Boolean = true
) {

    companion object {
        private const val MAX_EMBED_DESCRIPTION_LENGTH = 4096
    }

    var message: String = "content"
    var username: String = "BreweryX Updates"
    var avatarUrl: String = "https://github.com/breweryteam.png"
    var embedTitle: String = "Embed Title"
    var embedDescription: String = "Embed Description"
    var embedColor: String = "F5E083"
    var embedThumbnailUrl: String? = if (defaultThumbnail) avatarUrl else null
    var embedImageUrl: String? = null

    private fun hexStringToInt(hex: String): Int {
        val hexWithoutPrefix = hex.removePrefix("#")
        return hexWithoutPrefix.toInt(16)
    }

    private fun buildToJson(): String {
        val json = JsonObject()
        json.addProperty("username", username)
        json.addProperty("avatar_url", avatarUrl)
        json.addProperty("content", message)

        val embed = JsonObject()
        embed.addProperty("title", embedTitle)
        embed.addProperty("description", embedDescription)
        embed.addProperty("color", hexStringToInt(embedColor))

        embedThumbnailUrl?.let {
            val thumbnail = JsonObject()
            thumbnail.addProperty("url", it)
            embed.add("thumbnail", thumbnail)
        }

        embedImageUrl?.let {
            val image = JsonObject()
            image.addProperty("url", it)
            embed.add("image", image)
        }

        val embeds = JsonArray()
        createEmbeds().forEach(embeds::add)

        json.add("embeds", embeds)
        return json.toString()
    }

    private fun createEmbeds(): List<JsonObject> {
        if (embedDescription.length <= MAX_EMBED_DESCRIPTION_LENGTH) {
            return listOf(JsonObject().apply {
                addProperty("title", embedTitle)
                addProperty("description", embedDescription)
                addProperty("color", embedColor.toInt(16))
                embedThumbnailUrl?.let {
                    val thumbnail = JsonObject()
                    thumbnail.addProperty("url", it)
                    add("thumbnail", thumbnail)
                }
                embedImageUrl?.let {
                    val image = JsonObject()
                    image.addProperty("url", it)
                    add("image", image)
                }
            })
        }
        val embeds = mutableListOf<JsonObject>()
        var description = embedDescription
        while (description.isNotEmpty()) {
            val chunkLength = minOf(MAX_EMBED_DESCRIPTION_LENGTH, description.length)
            val chunk = description.substring(0, chunkLength)
            description = description.substring(chunkLength)
            embeds.add(JsonObject().apply {
                addProperty("title", embedTitle)
                addProperty("description", chunk)
                addProperty("color", embedColor.toInt(16))
                embedThumbnailUrl?.let {
                    val thumbnail = JsonObject()
                    thumbnail.addProperty("url", it)
                    add("thumbnail", thumbnail)
                }
                embedImageUrl?.let {
                    val image = JsonObject()
                    image.addProperty("url", it)
                    add("image", image)
                }
            })
        }
        return embeds
    }

    fun send() {
        val url = URI(webhookUrl).toURL()
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.outputStream.use { outputStream ->
            outputStream.write(buildToJson().toByteArray())

            val responseCode = connection.responseCode
            println("POST Response Code :: $responseCode")
            if (responseCode == HttpURLConnection.HTTP_OK) {
                println("Message sent successfully.")
            } else {
                println("Failed to send message.")
            }
        }
    }
}
