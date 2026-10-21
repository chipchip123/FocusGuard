package com.example.focusguard

import org.junit.Assert.*
import org.junit.Test

class VocabularyTest {
    @Test fun `questions always have four distinct answers and one correct answer`() {
        val words = listOf(Word("cat", "mèo", "noun"), Word("dog", "chó", "noun"),
            Word("bird", "chim", "noun"), Word("fish", "cá", "noun"), Word("kitten", "mèo", "noun"))
        var english = false
        var vietnamese = false
        repeat(100) { seed ->
            val q = Vocabulary.question(words, seed)
            assertEquals(4, q.choices.size)
            assertEquals(4, q.choices.distinct().size)
            assertEquals(1, q.choices.count { it == q.correct })
            if (q.prompt.startsWith("Nghĩa")) english = true else vietnamese = true
        }
        assertTrue(english && vietnamese)
    }
}
