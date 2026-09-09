package ch.bbw.m450.tictactoe;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Test helper: builds TicTacToe boards from a readable string layout.
 * <p>
 * Instead of assigning nine array slots by hand, a test can write the board the way it looks:
 *
 * <pre>
 * boardFrom("XXX",
 *           "OO.",
 *           "...");
 * </pre>
 *
 * All methods are {@code static} so they can also be used from a {@code @MethodSource}.
 */
public final class TestBoards {

	/** Marks an empty field in a string layout. */
	private static final char EMPTY = '.';

	/** Side length of the board (3x3). */
	private static final int SIDE = 3;

	private TestBoards() {
		// utility class, not meant to be instantiated
	}

	/**
	 * @return a board with no stones on it
	 */
	public static Stone[] emptyBoard() {
		return new Stone[TicTacToeMain.BOARD_SIZE];
	}

	/**
	 * Builds a board from three rows of three characters each.
	 *
	 * @param rows one string per row, using 'X' for CROSS, 'O' for CIRCLE and '.' for an empty field
	 * @return the board as the flat array the game logic expects (index = row * 3 + column)
	 */
	public static Stone[] boardFrom(String... rows) {
		if (rows.length != SIDE) {
			throw new IllegalArgumentException("expected " + SIDE + " rows, but got " + rows.length);
		}
		var board = emptyBoard();
		for (var row = 0; row < SIDE; row++) {
			var cells = rows[row];
			if (cells.length() != SIDE) {
				throw new IllegalArgumentException("row " + row + " must have " + SIDE + " cells, but was: " + cells);
			}
			for (var column = 0; column < SIDE; column++) {
				board[row * SIDE + column] = stoneOf(cells.charAt(column));
			}
		}
		return board;
	}

	private static Stone stoneOf(char cell) {
		return switch (cell) {
			case 'X' -> Stone.CROSS;
			case 'O' -> Stone.CIRCLE;
			case EMPTY -> null;
			default -> throw new IllegalArgumentException("unknown field '" + cell + "', expected 'X', 'O' or '.'");
		};
	}
}
