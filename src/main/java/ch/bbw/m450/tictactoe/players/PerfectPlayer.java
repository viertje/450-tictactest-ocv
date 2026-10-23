package ch.bbw.m450.tictactoe.players;

import java.util.Arrays;
import java.util.Objects;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer;

/**
 * Perfect player: never loses, and wins whenever the opponent lets it.
 * <p>
 * Uses minimax in its "negamax" form. Every position is rated from the point of view of the
 * player to move (+1 win, 0 draw, -1 loss, assuming both sides play their best). What is good
 * for one side is exactly as bad for the other, hence the sign flip in {@link #scoreOfMove}.
 * The whole game tree of 3x3 tic-tac-toe is small enough (a few hundred thousand positions)
 * to search completely, so there is no pruning and no depth limit.
 * <p>
 * Among equally good moves it plays the first (top-left-most) one, so it is deterministic.
 * It does not prefer quicker wins: it still wins, just not always as fast as possible.
 */
public class PerfectPlayer implements TicTacToePlayer {

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		var work = board.clone(); // the search places and removes stones: never touch the caller's board
		var bestMove = -1;
		var bestScore = Integer.MIN_VALUE;
		for (var i = 0; i < TicTacToeMain.BOARD_SIZE; i++) {
			if (work[i] == null) {
				var score = scoreOfMove(work, i, colorToPlay);
				if (score > bestScore) {
					bestScore = score;
					bestMove = i;
				}
			}
		}
		if (bestMove < 0) {
			throw new IllegalStateException("cannot play at all");
		}
		return bestMove;
	}

	/**
	 * @return the value for {@code me} of putting a stone on {@code position}, assuming perfect play from then on
	 */
	private static int scoreOfMove(Stone[] board, int position, Stone me) {
		board[position] = me;
		int score;
		if (TicTacToeMain.isWin(board, me)) {
			score = 1;
		} else if (isFull(board)) {
			score = 0;
		} else {
			score = -valueOfBestMove(board, me.opponent()); // the opponent's gain is my loss
		}
		board[position] = null; // undo, so the next candidate starts from the same board
		return score;
	}

	/**
	 * @return the best value the player {@code me} can still reach on this unfinished board
	 */
	private static int valueOfBestMove(Stone[] board, Stone me) {
		var best = -1; // worst case: lose
		for (var i = 0; i < TicTacToeMain.BOARD_SIZE; i++) {
			if (board[i] == null) {
				best = Math.max(best, scoreOfMove(board, i, me));
			}
		}
		return best;
	}

	private static boolean isFull(Stone[] board) {
		return Arrays.stream(board).allMatch(Objects::nonNull);
	}
}
