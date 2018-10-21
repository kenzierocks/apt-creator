package net.octyl.aptcreator.processor

internal fun replaceGeneratedImport(sourceLines: List<String>): List<String> {
    var firstImport = Integer.MAX_VALUE
    var lastImport = -1
    for ((i, line) in sourceLines.withIndex()) {
        if (line.startsWith("import ") && !line.startsWith("import static ")) {
            firstImport = Math.min(firstImport, i)
            lastImport = Math.max(lastImport, i)
        }
    }
    return when {
        lastImport >= 0 -> {
            val mutCopy = sourceLines.toMutableList()
            val importLines = mutCopy.subList(firstImport, lastImport + 1)
            importLines.replaceAll { line ->
                if (line.startsWith("import javax.annotation.Generated;"))
                    "import javax.annotation.processing.Generated;"
                else
                    line
            }
            importLines.sort()
            mutCopy
        }
        else -> sourceLines
    }
}
