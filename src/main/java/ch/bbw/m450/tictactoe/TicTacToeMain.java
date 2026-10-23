package ch.bbw.m450.tictactoe;

import java.util.Arrays;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;
import ch.bbw.m450.tictactoe.players.HumanPlayer;

/**
 * A small tic-tac-toe board. Initially with a human and a primitive but valid computer player.
 */
// The console IS this program's user interface (and what the E2E tests assert on), not
// diagnostic logging. A logger would be the wrong tool, so S106 is suppressed on purpose.
@SuppressWarnings("java:S106")
public class TicTacToeMain {

	public static final int BOARD_SIZE = 9;

	@SuppressWarnings("java:S1172") // required entry-point signature; args intentionally unused
	public static void main(String[] args) {
		play(new HumanPlayer(), new GreedyPlayer());
	}

	/**
	 * @param b TicTacToe-board to check for a winner
	 * @param color either X or O
	 * @return true if the colorToPlay wins the current board (has three in a line)
	 */
	public static boolean isWin(Stone[] b, Stone color) {
		// @formatter:off
		return  b[0] == color && b[0] == b[1] && b[1] == b[2] ||
				b[3] == color && b[3] == b[4] && b[4] == b[5] ||
				b[6] == color && b[6] == b[7] && b[7] == b[8] ||
				b[0] == color && b[0] == b[3] && b[3] == b[6] ||
				b[1] == color && b[1] == b[4] && b[4] == b[7] ||
				b[2] == color && b[2] == b[5] && b[5] == b[8] ||
				b[0] == color && b[0] == b[4] && b[4] == b[8] ||
				b[2] == color && b[2] == b[4] && b[4] == b[6];
		// @formatter:on
	}

	public static String toString(Stone[] board) {
		var sb = new StringBuilder();
		for (var i = 0; i < 3; i++) {
			for (var j = 0; j < 3; j++) {
				var index = i * 3 + j;
				String color = board[index] == Stone.CROSS ? "X" : "O";
				color = "\033[1m" + color + "\033[0m";
				color = board[index] == null ? "\033[37m" + index + "\033[0m" : color;
				sb.append(color)
						.append("  ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	/**
	 * The main game loop to play a game of tic-tac-toe with two opponents.
	 *
	 * @param xPlayer the player to start
	 * @param oPlayer the 2nd player. Must not be the same as xPlayer
	 * @return the winning Color or `null` on draw
	 */
	public static Stone play(TicTacToePlayer xPlayer, TicTacToePlayer oPlayer) {
		// the board is organized as a 1-dimensional matrix of size 3x3=9
		// to get the index for position at row r and colum c, calculate index=r*3+c
		//     0 | 1 | 2
		//    ---+---+---
		//     3 | 4 | 5
		//    ---+---+---
		//     6 | 7 | 8
		if(xPlayer == oPlayer) {
			throw new IllegalArgumentException("players must differ");
		}
		var board = new Stone[BOARD_SIZE]; // all null -> empty
		var currentPlayer = xPlayer;
		for (var round = 0; round < BOARD_SIZE; round++) { // it lasts for 9 rounds
			var currentColor = currentPlayer == xPlayer ? Stone.CROSS : Stone.CIRCLE;
			// we copy to avoid side effects by the player (not needed if they behave)
			var clone = Arrays.copyOf(board, board.length);
			var playTo = currentPlayer.play(clone, currentColor);
			if (playTo < 0 || playTo >= 9 || board[playTo] != null) {
				System.out.println(toString(board));
				throw new IllegalStateException("cannot play to position " + playTo);
			}
			board[playTo] = currentColor; // do the move
			if (isWin(board, currentColor)) { // game over and current player wins
				System.out.println(toString(board) + "...and the winner is: " + currentColor);
				return currentColor;
			}
			currentPlayer = currentPlayer == xPlayer ? oPlayer : xPlayer;
		}
		System.out.println("it's a draw!");
		return null;
	}

}
