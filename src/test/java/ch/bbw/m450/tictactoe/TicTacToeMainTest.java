package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.TestBoards.boardFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

/**
 * Tests for {@link TicTacToeMain}.
 * <p>
 * Board layouts are built by the {@link TestBoards} helper, repeated assertions are wrapped in
 * {@link #assertWins} / {@link #assertDoesNotWin}, and the mutable starting state is rebuilt by the
 * {@link #setUp} fixture before every test.
 * <p>
 * Constellations that differ only in their data are covered by parameterized tests, so that adding
 * another board means adding one line instead of one method.
 */
class TicTacToeMainTest {

	// ------------------------------------------------------------------
	// Fixture: rebuilt before every test, so no test can influence another
	// ------------------------------------------------------------------

	private Stone[] emptyBoard;

	private TicTacToePlayer xPlayer;

	private TicTacToePlayer oPlayer;

	@BeforeEach
	void setUp() {
		emptyBoard = TestBoards.emptyBoard();
		xPlayer = new GreedyPlayer();
		oPlayer = new GreedyPlayer();
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private void assertWins(Stone[] board, Stone color) {
		assertThat(TicTacToeMain.isWin(board, color))
				.as("%s should win on this board:%n%s", color, TicTacToeMain.toString(board))
				.isTrue();
	}

	private void assertDoesNotWin(Stone[] board, Stone color) {
		assertThat(TicTacToeMain.isWin(board, color))
				.as("%s should not win on this board:%n%s", color, TicTacToeMain.toString(board))
				.isFalse();
	}

	/**
	 * @return a board where the given color owns the whole top row
	 */
	private static Stone[] topRowWinFor(Stone color) {
		return boardFrom(color == Stone.CROSS ? "XXX" : "OOO",
				"...",
				"...");
	}

	// ------------------------------------------------------------------
	// isWin: every winning line, supplied as complex objects (Stone[])
	// ------------------------------------------------------------------

	/**
	 * Data source for {@link #detectsEveryWinningLine}. Must be static, and must not be private,
	 * so that JUnit can call it.
	 */
	static Stream<Arguments> winningLinesForCross() {
		return Stream.of(
				arguments("top row", boardFrom("XXX",
						"OO.",
						"...")),
				arguments("middle row", boardFrom("OO.",
						"XXX",
						"...")),
				arguments("bottom row", boardFrom(".OO",
						"...",
						"XXX")),
				arguments("left column", boardFrom("X..",
						"XOO",
						"X..")),
				arguments("middle column", boardFrom(".X.",
						"OXO",
						".X.")),
				arguments("right column", boardFrom("..X",
						"OOX",
						"..X")),
				arguments("diagonal", boardFrom("X..",
						"OXO",
						"..X")),
				arguments("anti-diagonal", boardFrom("..X",
						"OXO",
						"X..")));
	}

	@ParameterizedTest(name = "CROSS wins on the {0}")
	@MethodSource("winningLinesForCross")
	void detectsEveryWinningLine(String line, Stone[] board) {
		assertWins(board, Stone.CROSS);
	}

	// ------------------------------------------------------------------
	// isWin: constellations that must NOT count as a win
	// ------------------------------------------------------------------

	@ParameterizedTest(name = "no win for CROSS on [{0}][{1}][{2}]")
	@CsvSource({
			"..., ..., ...",  // empty board
			"XX., OO., ...",  // only two in a row
			"XXO, ..., ...",  // line blocked by the opponent
			"X.X, OO., ...",  // gap in the middle of the row
			"X.., .X., ...",  // scattered stones, no line
			"OOO, XX., ...",  // CIRCLE wins here, CROSS does not
	})
	void detectsNoWinOnIncompleteLines(String top, String middle, String bottom) {
		assertDoesNotWin(boardFrom(top, middle, bottom), Stone.CROSS);
	}

	@Test
	void emptyBoardHasNoWinnerAtAll() {
		assertDoesNotWin(emptyBoard, Stone.CROSS);
		assertDoesNotWin(emptyBoard, Stone.CIRCLE);
	}

	// ------------------------------------------------------------------
	// isWin: the same rule must hold for both colors
	// ------------------------------------------------------------------

	@ParameterizedTest(name = "a top row of {0} is a win for {0} only")
	@EnumSource(Stone.class)
	void aWinForOneColorIsNeverAWinForTheOpponent(Stone color) {
		var board = topRowWinFor(color);
		assertWins(board, color);
		assertDoesNotWin(board, color.opponent());
	}

	// ------------------------------------------------------------------
	// play: full games
	// ------------------------------------------------------------------

	@Test
	void twoGreedyPlayersLetTheStartingPlayerWin() {
		assertThat(TicTacToeMain.play(xPlayer, oPlayer)).isEqualTo(Stone.CROSS);
	}

	@Test
	void aFullBoardWithoutThreeInALineIsADraw() {
		var scriptedX = new ScriptedPlayer(0, 2, 3, 7, 8);
		var scriptedO = new ScriptedPlayer(1, 4, 5, 6);
		assertThat(TicTacToeMain.play(scriptedX, scriptedO)).isNull();
	}

	// ------------------------------------------------------------------
	// play: negative cases and boundary values
	// ------------------------------------------------------------------

	@Test
	void theSamePlayerTwiceIsRejected() {
		assertThatThrownBy(() -> TicTacToeMain.play(xPlayer, xPlayer))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("players must differ");
	}

	/**
	 * Valid positions are 0..8, so -1 and 9 are the two boundary values just outside the board.
	 * The extremes are added to show that the check does not overflow.
	 */
	@ParameterizedTest(name = "position {0} is outside the board")
	@ValueSource(ints = {-1, TicTacToeMain.BOARD_SIZE, Integer.MIN_VALUE, Integer.MAX_VALUE})
	void playingOutsideTheBoardIsRejected(int position) {
		assertThatThrownBy(() -> TicTacToeMain.play(new ScriptedPlayer(position), oPlayer))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("cannot play to position " + position);
	}

	@Test
	void playingToAnOccupiedFieldIsRejected() {
		assertThatThrownBy(() -> TicTacToeMain.play(new ScriptedPlayer(0), new ScriptedPlayer(0)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("cannot play to position 0");
	}
}
