package com.example.focusguard

import android.content.Context
import kotlin.random.Random

data class Word(val english: String, val vietnamese: String, val part: String)
data class Question(val prompt: String, val correct: String, val choices: List<String>)

object Vocabulary {
    fun load(context: Context): List<Word> = context.assets.open("english_vocabulary.csv").bufferedReader().useLines { lines ->
        lines.drop(1).mapNotNull { line ->
            val p = line.split(',')
            if (p.size == 3 && p.all { it.isNotBlank() }) Word(p[0].trim(), p[1].trim(), p[2].trim()) else null
        }.distinctBy { it.english }.toList()
    }
    fun question(words: List<Word>, seed: Int): Question {
        val random = Random(seed)
        val word = words.random(random)
        val toVietnamese = random.nextBoolean()
        fun answer(w: Word) = if (toVietnamese) w.vietnamese else w.english
        val correct = answer(word)
        val choices = (words.map(::answer).distinct().filter { it != correct }.shuffled(random).take(3) + correct).shuffled(random)
        return Question(if (toVietnamese) "Nghĩa của “${word.english}” là gì?" else "Từ nào có nghĩa là “${word.vietnamese}”?", correct, choices)
    }
}
