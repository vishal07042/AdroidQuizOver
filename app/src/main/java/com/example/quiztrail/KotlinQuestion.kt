package com.example.quiztrail

data class KotlinQuestion(
    val id: Int,
    val title: String,
    val text: String?,
    val code2: String?,
    val choices: List<String>,
    val answer: String
)
