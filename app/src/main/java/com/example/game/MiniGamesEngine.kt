package com.example.game

import com.example.model.MATH_QUIZ_QUESTIONS_PER_LEVEL
import com.example.model.TAP_TAP_TARGET_TAPS
import kotlin.random.Random

enum class MathOperation(val symbol: String, val label: String) {
    ADD("+", "Addition"),
    SUBTRACT("-", "Subtraction"),
    MULTIPLY("×", "Multiplication"),
    DIVIDE("÷", "Division")
}

data class MathQuestion(
    val leftOperand: Int,
    val rightOperand: Int,
    val operation: MathOperation,
    val correctAnswer: Int,
    val options: List<Int>
) {
    val promptText: String
        get() = "$leftOperand ${operation.symbol} $rightOperand = ?"
}

object MathQuizEngine {

    fun generateLevelQuestions(
        levelNumber: Int,
        count: Int = MATH_QUIZ_QUESTIONS_PER_LEVEL,
        random: Random = Random.Default
    ): List<MathQuestion> {
        val operationsPool = if (count >= 4) {
            val base = mutableListOf(
                MathOperation.ADD,
                MathOperation.SUBTRACT,
                MathOperation.MULTIPLY,
                MathOperation.DIVIDE
            )
            while (base.size < count) {
                base += MathOperation.entries[random.nextInt(MathOperation.entries.size)]
            }
            base.shuffled(random)
        } else {
            List(count) { MathOperation.entries[random.nextInt(MathOperation.entries.size)] }
        }

        return operationsPool.map { op ->
            createQuestion(op = op, levelNumber = levelNumber, random = random)
        }
    }

    fun createQuestion(
        op: MathOperation,
        levelNumber: Int,
        random: Random = Random.Default
    ): MathQuestion {
        val easyScale = ((levelNumber - 1).coerceIn(0, 10)) * 2
        val (left, right, answer) = when (op) {
            MathOperation.ADD -> {
                val a = random.nextInt(2, 16 + easyScale)
                val b = random.nextInt(1, 13 + easyScale / 2)
                Triple(a, b, a + b)
            }
            MathOperation.SUBTRACT -> {
                val b = random.nextInt(1, 11 + easyScale / 2)
                val diff = random.nextInt(1, 15 + easyScale / 2)
                Triple(b + diff, b, diff)
            }
            MathOperation.MULTIPLY -> {
                val a = random.nextInt(2, 10)
                val b = random.nextInt(1, 6 + (levelNumber.coerceAtMost(5)))
                Triple(a, b, a * b)
            }
            MathOperation.DIVIDE -> {
                val divisor = random.nextInt(2, 10)
                val quotient = random.nextInt(1, 10 + (levelNumber / 2).coerceAtMost(3))
                Triple(divisor * quotient, divisor, quotient)
            }
        }

        val optionsSet = linkedSetOf(answer)
        val offsets = listOf(-4, -3, -2, -1, 1, 2, 3, 4, 5, 6, 8, 10).shuffled(random)
        for (delta in offsets) {
            if (optionsSet.size >= 4) break
            val candidate = answer + delta
            if (candidate >= 0) {
                optionsSet += candidate
            }
        }
        var fallback = 1
        while (optionsSet.size < 4) {
            optionsSet += (answer + fallback).coerceAtLeast(fallback)
            fallback++
        }

        return MathQuestion(
            leftOperand = left,
            rightOperand = right,
            operation = op,
            correctAnswer = answer,
            options = optionsSet.toList().shuffled(random)
        )
    }

    fun isTapTapGoalReached(taps: Int): Boolean = taps >= TAP_TAP_TARGET_TAPS
}
