package com.example.game

import com.example.model.MatchOutcome
import kotlin.random.Random

enum class CellMark {
    EMPTY,
    PLAYER_X,
    BOT_O
}

data class BoardEvaluation(
    val outcome: MatchOutcome?,
    val winningLine: List<Int> = emptyList()
)

object TicTacToeEngine {

    private val winningLines = listOf(
        listOf(0, 1, 2),
        listOf(3, 4, 5),
        listOf(6, 7, 8),
        listOf(0, 3, 6),
        listOf(1, 4, 7),
        listOf(2, 5, 8),
        listOf(0, 4, 8),
        listOf(2, 4, 6)
    )

    fun evaluate(board: List<CellMark>): BoardEvaluation {
        for (line in winningLines) {
            val a = board[line[0]]
            val b = board[line[1]]
            val c = board[line[2]]
            if (a != CellMark.EMPTY && a == b && b == c) {
                val outcome = if (a == CellMark.PLAYER_X) MatchOutcome.WIN else MatchOutcome.LOSS
                return BoardEvaluation(outcome = outcome, winningLine = line)
            }
        }
        return if (board.none { it == CellMark.EMPTY }) {
            BoardEvaluation(outcome = MatchOutcome.DRAW, winningLine = emptyList())
        } else {
            BoardEvaluation(outcome = null, winningLine = emptyList())
        }
    }

    /**
     * Medium-difficulty bot:
     * 1. Always takes an immediate winning move if available.
     * 2. Blocks the player's immediate winning threat 85% of the time.
     * 3. Takes center (75%) or builds 2-in-a-row / strategic corners (70%),
     *    with occasional tactical openings so players can beat it with forks.
     */
    fun chooseMediumBotMove(
        board: List<CellMark>,
        random: Random = Random.Default
    ): Int {
        val emptyCells = board.indices.filter { board[it] == CellMark.EMPTY }
        if (emptyCells.isEmpty()) return -1

        // 1. Always capitalize on immediate winning move
        findWinningMove(board, CellMark.BOT_O)?.let { return it }

        // 2. Block player's immediate threat 85% of the time
        val playerWinMove = findWinningMove(board, CellMark.PLAYER_X)
        if (playerWinMove != null && random.nextFloat() < 0.85f) {
            return playerWinMove
        }

        // 3. Control center strategically (75% chance)
        if (board[4] == CellMark.EMPTY && random.nextFloat() < 0.75f) {
            return 4
        }

        // 4. Create a two-in-a-row threat or take an open corner (70% chance)
        if (random.nextFloat() < 0.70f) {
            val setupMoves = findTwoInRowSetup(board, CellMark.BOT_O)
            if (setupMoves.isNotEmpty()) {
                return setupMoves.random(random)
            }
            val openCorners = listOf(0, 2, 6, 8).filter { board[it] == CellMark.EMPTY }
            if (openCorners.isNotEmpty()) {
                return openCorners.random(random)
            }
        }

        return emptyCells.random(random)
    }

    private fun findWinningMove(board: List<CellMark>, mark: CellMark): Int? {
        for (line in winningLines) {
            val marks = line.map { board[it] }
            if (marks.count { it == mark } == 2 && marks.count { it == CellMark.EMPTY } == 1) {
                return line.first { board[it] == CellMark.EMPTY }
            }
        }
        return null
    }

    private fun findTwoInRowSetup(board: List<CellMark>, mark: CellMark): List<Int> {
        val candidates = mutableSetOf<Int>()
        for (line in winningLines) {
            val marks = line.map { board[it] }
            if (marks.count { it == mark } == 1 && marks.count { it == CellMark.EMPTY } == 2) {
                line.filter { board[it] == CellMark.EMPTY }.forEach { candidates.add(it) }
            }
        }
        return candidates.toList()
    }
}
