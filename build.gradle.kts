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

import org.apache.commons.io.output.ByteArrayOutputStream
import org.apache.tools.ant.filters.ReplaceTokens
import java.nio.charset.Charset

plugins {
    id("java")
    id("maven-publish")
    id("com.gradleup.shadow") version "8.3.5"
}

val langVersion = 17
val encoding = "UTF-8"

group = "com.dre.brewery"
version = "3.4.3-SNAPSHOT"

repositories {
    mavenCentral()
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
}

dependencies {
    // Spigot
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT") {
        exclude("com.google.code.gson", "gson") // Implemented manually
    }
    // Implemented manually mainly due to older server versions implementing versions of GSON
    // Which don't support records.
    implementation("com.google.code.gson:gson:2.11.0")
    // For proper scheduling between Bukkit-Folia like servers, https://github.com/Anon8281/UniversalScheduler
    implementation("com.github.Anon8281:UniversalScheduler:0.1.3")
    // Nice annotations, I prefer these to Lombok's, https://www.jetbrains.com/help/idea/annotating-source-code.html
    implementation("org.jetbrains:annotations:16.0.2")
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
    compileOnly("com.griefcraft.lwc:LWCX:2.2.9-dev")// https://www.spigotmc.org/resources/lwc-extended.69551/history
    compileOnly("com.github.TechFortress:GriefPrevention:16.18") // https://www.spigotmc.org/resources/griefprevention.1884/history
    compileOnly("de.diddiz:logblock:1.16.5.1") // https://www.spigotmc.org/resources/logblock.67333/history
    compileOnly("com.github.Slimefun:Slimefun4:RC-35") // https://github.com/Slimefun/Slimefun4/releases
    compileOnly("io.lumine:MythicLib-dist:1.6-SNAPSHOT") // https://www.spigotmc.org/resources/mythiclib.90306/history
    compileOnly("com.acrobot.chestshop:chestshop:3.12.2") // https://github.com/ChestShop-authors/ChestShop-3/releases
    compileOnly("com.palmergames.bukkit.towny:towny:0.100.3.0") // https://www.spigotmc.org/resources/towny-advanced.72694/history
    compileOnly("com.nisovin.shopkeepers:ShopkeepersAPI:2.18.0") // https://www.spigotmc.org/resources/shopkeepers.80756/history
    compileOnly("nl.rutgerkok:blocklocker:1.10.4") // https://www.spigotmc.org/resources/blocklocker.3268/history
    compileOnly("me.clip:placeholderapi:2.11.5") // https://www.spigotmc.org/resources/placeholderapi.6245/history
    compileOnly("io.th0rgal:oraxen:1.163.0") // https://www.spigotmc.org/resources/%E2%98%84%EF%B8%8F-oraxen-custom-items-blocks-emotes-furniture-resourcepack-and-gui-1-18-1-21-3.72448/
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.6.1") // https://www.spigotmc.org/resources/%E2%9C%A8itemsadder%E2%AD%90emotes-mobs-items-armors-hud-gui-emojis-blocks-wings-hats-liquids.73355/updates



    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}



tasks {

    build {
        dependsOn(shadowJar)
        finalizedBy("kotlinReducedJar")
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
		relocate("com.google", "com.dre.brewery.depend.google")
        relocate("com.github.Anon8281.universalScheduler", "com.dre.brewery.depend.universalScheduler")
        relocate("eu.okaeri", "com.dre.brewery.depend.okaeri")
        relocate("com.mongodb", "com.dre.brewery.depend.mongodb")

        archiveClassifier.set("")
    }

    // Kotlin Reduced Jars
	register<Copy>("prepareKotlinReducedJar") {
		dependsOn(shadowJar)
		from(zipTree(shadowJar.get().archiveFile))
		into(layout.buildDirectory.dir("kt-reduced"))
		doLast {
			val pluginFile = layout.buildDirectory.file("kt-reduced/plugin.yml").get().asFile
			var content = pluginFile.readText()
			content = content.replace("libraries: ['org.jetbrains.kotlin:kotlin-stdlib:2.0.21']", "")
			pluginFile.writeText(content)
		}
	}

	register<Jar>("kotlinReducedJar") {
		dependsOn("prepareKotlinReducedJar")
		from(layout.buildDirectory.dir("kt-reduced"))
		include("**/*")
		duplicatesStrategy = DuplicatesStrategy.INHERIT
		archiveClassifier.set("KtReduced")
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

java {
    toolchain.languageVersion = JavaLanguageVersion.of(langVersion)
}


publishing {
    val user = System.getenv("repo_username")
    val pass = System.getenv("repo_secret")

    repositories {
        if (user != null && pass != null) {
            maven {
                name = "jsinco-repo"
                url = uri("https://repo.jsinco.dev/releases")
                credentials(PasswordCredentials::class) {
                    username = user
                    password = pass
                }
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }

	publications {
		create<MavenPublication>("maven") { // Jitpack
			artifact(tasks.shadowJar.get().archiveFile) {
				builtBy(tasks.shadowJar)
			}
		}

        if (user != null && pass != null) {
            create<MavenPublication>("jsinco") {
                groupId = "com.dre.brewery"
                artifactId = "BreweryX"
                version = project.version.toString()
                artifact(tasks.shadowJar.get().archiveFile) {
                    builtBy(tasks.shadowJar)
                }
            }
        }
	}
}
