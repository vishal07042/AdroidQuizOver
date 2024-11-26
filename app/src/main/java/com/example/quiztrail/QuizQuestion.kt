package com.example.quiztrail

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswer: Int
)
