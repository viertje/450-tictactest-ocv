package ch.bbw.m450.tictactoe;

import static ch.bbw.m450.tictactoe.TestBoards.boardFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.GreedyPlayer;

/**
 * Tests for {@link TicTacToeMain}.
 * <p>
 * The board layouts are built by the {@link TestBoards} helper, the repeated assertions are wrapped
 * in {@link #assertWins} / {@link #assertDoesNotWin}, and the mutable starting state is rebuilt by
 * the {@link #setUp} fixture before every single test.
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
	// Helpers: readable assertions
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

	// ------------------------------------------------------------------
	// isWin: positive cases, one per winning line
	// ------------------------------------------------------------------

	@Test
	void detectsTopRowWin() {
		assertWins(boardFrom("XXX",
				"OO.",
				"..."), Stone.CROSS);
	}

	@Test
	void detectsMiddleRowWin() {
		assertWins(boardFrom("OO.",
				"XXX",
				"..."), Stone.CROSS);
	}

	@Test
	void detectsBottomRowWin() {
		assertWins(boardFrom(".OO",
				"...",
				"XXX"), Stone.CROSS);
	}

	@Test
	void detectsLeftColumnWin() {
		assertWins(boardFrom("X..",
				"XOO",
				"X.."), Stone.CROSS);
	}

	@Test
	void detectsMiddleColumnWin() {
		assertWins(boardFrom(".X.",
				"OXO",
				".X."), Stone.CROSS);
	}

	@Test
	void detectsRightColumnWin() {
		assertWins(boardFrom("..X",
				"OOX",
				"..X"), Stone.CROSS);
	}

	@Test
	void detectsDiagonalWin() {
		assertWins(boardFrom("X..",
				"OXO",
				"..X"), Stone.CROSS);
	}

	@Test
	void detectsAntiDiagonalWin() {
		assertWins(boardFrom("..X",
				"OXO",
				"X.."), Stone.CROSS);
	}

	@Test
	void detectsWinForCircleAsWell() {
		assertWins(boardFrom("OOO",
				"XX.",
				"..."), Stone.CIRCLE);
	}

	// ------------------------------------------------------------------
	// isWin: negative cases
	// ------------------------------------------------------------------

	@Test
	void emptyBoardHasNoWinner() {
		assertDoesNotWin(emptyBoard, Stone.CROSS);
		assertDoesNotWin(emptyBoard, Stone.CIRCLE);
	}

	@Test
	void twoInARowIsNotAWin() {
		assertDoesNotWin(boardFrom("XX.",
				"OO.",
				"..."), Stone.CROSS);
	}

	@Test
	void lineBlockedByTheOpponentIsNotAWin() {
		assertDoesNotWin(boardFrom("XXO",
				"...",
				"..."), Stone.CROSS);
	}

	@Test
	void aWinForOneColorIsNoWinForTheOther() {
		var crossWins = boardFrom("XXX",
				"OO.",
				"...");
		assertWins(crossWins, Stone.CROSS);
		assertDoesNotWin(crossWins, Stone.CIRCLE);
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

	@Test
	void playingBelowTheBoardIsRejected() {
		assertThatThrownBy(() -> TicTacToeMain.play(new ScriptedPlayer(-1), oPlayer))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("cannot play to position -1");
	}

	@Test
	void playingAboveTheBoardIsRejected() {
		assertThatThrownBy(() -> TicTacToeMain.play(new ScriptedPlayer(TicTacToeMain.BOARD_SIZE), oPlayer))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("cannot play to position 9");
	}

	@Test
	void playingToAnOccupiedFieldIsRejected() {
		assertThatThrownBy(() -> TicTacToeMain.play(new ScriptedPlayer(0), new ScriptedPlayer(0)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("cannot play to position 0");
	}
}
