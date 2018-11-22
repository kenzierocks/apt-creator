package net.octyl.aptcreator

import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

val APT_CREATOR_CLASSPATH: List<Path> =
        (System.getProperty("apt.creator.classpath")
                ?: throw IllegalStateException("No apt.creator.classpath provided!"))
                .splitToSequence(File.pathSeparatorChar)
                .map { Paths.get(it).toAbsolutePath() }
                .toList()
                .also {
                    println("Using compile classpath:")
                    it.forEach { println("\t$it") }
                }
