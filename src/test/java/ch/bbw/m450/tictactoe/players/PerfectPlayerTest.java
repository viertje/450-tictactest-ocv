package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.TestBoards.boardFrom;
import static ch.bbw.m450.tictactoe.TestBoards.emptyBoard;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Named.named;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Tests for {@link PerfectPlayer}. The central claim, "it never loses", is checked exhaustively:
 * 3x3 tic-tac-toe is small enough to let the opponent try every legal move in every game.
 */
class PerfectPlayerTest {

	/** Fixture: a fresh player before every test. */
	private PerfectPlayer player;

	/** Number of finished games played by {@link #playAllGames}, to prove the enumeration really ran. */
	private int gamesPlayed;

	@BeforeEach
	void setUp() {
		player = new PerfectPlayer();
		gamesPlayed = 0;
	}

	/**
	 * Positions with exactly one sensible move: either win right now, or block the opponent's
	 * win (every other move loses at once).
	 */
	static Stream<Arguments> forcedMoves() {
		return Stream.of(
				arguments(named("X completes the top row", boardFrom("XX.",
						"OO.",
						"...")), Stone.CROSS, 2),
				arguments(named("O completes the top row", boardFrom("OO.",
						"XX.",
						"X..")), Stone.CIRCLE, 2),
				arguments(named("X must block O's top row", boardFrom("OO.",
						"X..",
						".X.")), Stone.CROSS, 2),
				arguments(named("O must block X's top row", boardFrom("XX.",
						"O.X",
						".O.")), Stone.CIRCLE, 2));
	}

	@ParameterizedTest
	@MethodSource("forcedMoves")
	void findsTheOnlyMoveThatDoesNotThrowTheGameAway(Stone[] board, Stone colorToPlay, int expected) {
		assertThat(player.play(board, colorToPlay)).isEqualTo(expected);
	}

	@Test
	void playsTheFirstFieldWhenAllMovesAreEquallyGood() {
		// every opening move is a draw with perfect play, so the tie-break decides: first field
		assertThat(player.play(emptyBoard(), Stone.CROSS)).isEqualTo(0);
	}

	@Test
	void doesNotModifyTheBoardItIsGiven() {
		var board = boardFrom("X..",
				".O.",
				"...");
		var before = board.clone();
		player.play(board, Stone.CROSS);
		assertThat(board).containsExactly(before);
	}

	@Test
	void throwsWhenTheBoardIsFull() {
		assertThatThrownBy(() -> player.play(boardFrom("XOX",
				"OXO",
				"OXO"), Stone.CROSS))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("cannot play at all");
	}

	@ParameterizedTest(name = "never loses as {0}, whatever the opponent plays")
	@EnumSource(Stone.class)
	void neverLoses(Stone perfect) {
		var lostGames = new ArrayList<String>();

		playAllGames(emptyBoard(), Stone.CROSS, perfect, "", lostGames);

		assertThat(gamesPlayed).as("games played").isGreaterThan(100);
		assertThat(lostGames).as("games lost by the perfect player (as move sequences)").isEmpty();
	}

	@Test
	void twoPerfectPlayersEndInADraw() {
		assertThat(TicTacToeMain.play(player, new PerfectPlayer())).isNull();
	}

	@Test
	void beatsTheGreedyPlayerWhenStarting() {
		assertThat(TicTacToeMain.play(player, new GreedyPlayer())).isEqualTo(Stone.CROSS);
	}

	@Test
	void beatsTheGreedyPlayerWhenSecond() {
		assertThat(TicTacToeMain.play(new GreedyPlayer(), player)).isEqualTo(Stone.CIRCLE);
	}

	/**
	 * Plays every possible game in which the color {@code perfect} is played by the
	 * {@link PerfectPlayer} and the other color tries every legal move. Games won by the
	 * opponent are recorded in {@code lostGames} as their sequence of moves.
	 */
	private void playAllGames(Stone[] board, Stone toMove, Stone perfect, String moves, List<String> lostGames) {
		if (toMove == perfect) {
			playOn(board, player.play(board, perfect), toMove, perfect, moves, lostGames);
			return;
		}
		for (var i = 0; i < board.length; i++) {
			if (board[i] == null) {
				playOn(board, i, toMove, perfect, moves, lostGames);
			}
		}
	}

	private void playOn(Stone[] board, int move, Stone mover, Stone perfect, String moves, List<String> lostGames) {
		board[move] = mover;
		var movesSoFar = moves + move + " ";
		if (TicTacToeMain.isWin(board, mover)) {
			gamesPlayed++;
			if (mover != perfect) {
				lostGames.add(movesSoFar);
			}
		} else if (Arrays.stream(board).allMatch(Objects::nonNull)) {
			gamesPlayed++; // draw
		} else {
			playAllGames(board, mover.opponent(), perfect, movesSoFar, lostGames);
		}
		board[move] = null; // undo
	}
}
