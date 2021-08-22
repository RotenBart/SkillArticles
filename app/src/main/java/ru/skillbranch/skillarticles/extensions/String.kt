package ru.skillbranch.skillarticles.extensions

fun String?.indexesOf(substr: String, ignoreCase: Boolean = true): List<Int> {
    val indexes = mutableListOf<Int>()
    var index = 0
    while (index != -1) {
        this?.let { string ->
            index = string.indexOf(substr, index, ignoreCase)
        }
        if(index != -1) {
            indexes.add(index)
            index++
        }
    }
    return indexes
}