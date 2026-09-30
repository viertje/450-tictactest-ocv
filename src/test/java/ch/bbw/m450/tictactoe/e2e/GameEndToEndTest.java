package ch.bbw.m450.tictactoe.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junitpioneer.jupiter.StdIo;
import org.junitpioneer.jupiter.StdOut;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;
import ch.bbw.m450.tictactoe.players.HumanPlayer;

/**
 * End-to-end tests: drive the game exactly the way a real player would -- typing lines at a
 * console -- instead of calling internal methods directly. JUnit Pioneer's {@code @StdIo}
 * simulates {@code System.in}/{@code System.out} for that; each simulated move sequence is
 * chosen and verified by hand (both players' strategies are deterministic here) so the outcome
 * is predictable.
 * <p>
 * Tagged "e2e" so these run in their own Gradle task ({@code ./gradlew e2eTest}) instead of the
 * fast {@code test} task -- see {@code build.gradle}.
 */
@Tag("e2e")
class GameEndToEndTest {

	// ------------------------------------------------------------------
	// full game flow: first input to result
	// ------------------------------------------------------------------

	/**
	 * X (field 0), O (field 3), X (field 1), O (field 4), X (field 2) completes the top row for
	 * X -- neither player is ever offered a field that isn't actually free.
	 */
	@Test
	@StdIo({ "0", "3", "1", "4", "2" })
	void aFullGameEndsWithCrossWinningTheTopRow(StdOut out) {
		var winner = TicTacToeMain.play(new HumanPlayer(), new HumanPlayer());

		assertThat(winner).isEqualTo(Stone.CROSS);
		assertThat(out.capturedString()).contains("...and the winner is: " + Stone.CROSS);
	}

	/**
	 * X (0), O (3), X (1), O (4), X (8), O (5) completes the middle row for O.
	 */
	@Test
	@StdIo({ "0", "3", "1", "4", "8", "5" })
	void aFullGameEndsWithCircleWinningTheMiddleRow(StdOut out) {
		var winner = TicTacToeMain.play(new HumanPlayer(), new HumanPlayer());

		assertThat(winner).isEqualTo(Stone.CIRCLE);
		assertThat(out.capturedString()).contains("...and the winner is: " + Stone.CIRCLE);
	}

	/**
	 * All nine fields get filled (X: 0,2,4,6,8 -- O: 1,3,5,7) without any line ever completing
	 * for either color.
	 */
	@Test
	@StdIo({ "0", "1", "2", "4", "3", "5", "7", "6", "8" })
	void aFullGameEndsInADraw(StdOut out) {
		var winner = TicTacToeMain.play(new HumanPlayer(), new HumanPlayer());

		assertThat(winner).isNull();
		assertThat(out.capturedString()).contains("it's a draw!");
	}

	/**
	 * Goes through the real entry point ({@code main()}), not just the pieces -- it always pairs
	 * a human against the (deterministic, "first free field") {@code GreedyPlayer}, so a human
	 * playing 4, 8, 2, 6 forces a win on the 2-4-6 diagonal: GreedyPlayer fills 0, then 1, then 3
	 * in between, none of which block or complete anything.
	 */
	@Test
	@StdIo({ "4", "8", "2", "6" })
	void theRealEntryPointWiresAHumanAgainstTheGreedyPlayer(StdOut out) {
		TicTacToeMain.main(new String[0]);

		assertThat(out.capturedString())
				.contains("where to to put the next")
				.contains("...and the winner is: " + Stone.CROSS);
	}

	// ------------------------------------------------------------------
	// invalid and malicious input
	//
	// All of these are regression tests for a bug found while writing this suite: a HumanPlayer
	// used to let a bad keystroke crash the entire game with an uncaught NumberFormatException
	// (see Testkonzept.md). HumanPlayer now re-prompts instead -- these confirm that for several
	// different shapes of bad input, each ending with one valid move to prove the game actually
	// continues afterwards rather than just silently swallowing the error.
	// ------------------------------------------------------------------

	@Test
	@StdIo({ "not-a-number", "4" })
	void nonNumericTextIsRejectedAndRePrompted(StdOut out) {
		var position = new HumanPlayer().play(new Stone[TicTacToeMain.BOARD_SIZE], Stone.CROSS);

		assertThat(position).isEqualTo(4);
		assertThat(out.capturedString())
				.contains("'not-a-number' is not a valid number")
				.contains("where to to put the next");
	}

	@Test
	@StdIo({ "", "4" })
	void anEmptyLineIsRejectedAndRePrompted() {
		var position = new HumanPlayer().play(new Stone[TicTacToeMain.BOARD_SIZE], Stone.CROSS);

		assertThat(position).isEqualTo(4);
	}

	@Test
	@StdIo({ "4.5", "4" })
	void aDecimalNumberIsRejectedAndRePrompted() {
		var position = new HumanPlayer().play(new Stone[TicTacToeMain.BOARD_SIZE], Stone.CROSS);

		assertThat(position).isEqualTo(4);
	}

	@Test
	@StdIo({ "   ", "4" })
	void whitespaceOnlyInputIsRejectedAndRePrompted() {
		var position = new HumanPlayer().play(new Stone[TicTacToeMain.BOARD_SIZE], Stone.CROSS);

		assertThat(position).isEqualTo(4);
	}

	/**
	 * A number too large for an {@code int} is still just text as far as {@code Integer.parseInt}
	 * is concerned -- it throws the same {@code NumberFormatException} as any other unparsable
	 * input, so it takes the same re-prompt path rather than overflowing silently.
	 */
	@Test
	@StdIo({ "99999999999999999999999999", "4" })
	void aNumberTooLargeToFitInAnIntIsRejectedAndRePrompted() {
		var position = new HumanPlayer().play(new Stone[TicTacToeMain.BOARD_SIZE], Stone.CROSS);

		assertThat(position).isEqualTo(4);
	}

	/**
	 * A malicious-input variant: an attempt to inject a terminal escape sequence through the
	 * "move" prompt. It's rejected the same way as any other non-numeric text -- and since
	 * HumanPlayer only ever reads its own terminal's input back to itself (there's no other user
	 * or remote party this could affect), reflecting it in the "not a valid number" message is
	 * accepted here rather than treated as something that needs sanitizing.
	 */
	@Test
	@StdIo({ "\u001B[31mHACKED\u001B[0m", "4" })
	void aTerminalEscapeSequenceInTheInputIsRejectedAndRePrompted() {
		var position = new HumanPlayer().play(new Stone[TicTacToeMain.BOARD_SIZE], Stone.CROSS);

		assertThat(position).isEqualTo(4);
	}

	// ------------------------------------------------------------------
	// denial of service
	// ------------------------------------------------------------------

	/**
	 * Not {@code @StdIo} here: its simulated lines are Java string-literal constants, capped by
	 * the class-file constant-pool limit (~64 KB) -- far too small to build the kind of
	 * multi-megabyte "line" a real malicious paste could contain. {@code System.in} is swapped by
	 * hand instead, restored in {@code finally} either way.
	 * <p>
	 * The {@code @Timeout} is the actual safeguard the assignment asks for: {@code parseInt}
	 * already rejects this in one linear pass today, so this test currently passes quickly -- but
	 * if that ever regressed to something slower, this fails fast with a clear timeout instead of
	 * quietly stalling the whole CI run.
	 */
	@Test
	@Timeout(value = 5, unit = TimeUnit.SECONDS)
	void anExtremelyLongSingleLineIsRejectedQuicklyInsteadOfHanging() {
		var maliciousLine = "9".repeat(1_000_000) + System.lineSeparator() + "4" + System.lineSeparator();
		var originalIn = System.in;
		System.setIn(new ByteArrayInputStream(maliciousLine.getBytes(StandardCharsets.UTF_8)));
		try {
			var position = new HumanPlayer().play(new Stone[TicTacToeMain.BOARD_SIZE], Stone.CROSS);
			assertThat(position).isEqualTo(4);
		} finally {
			System.setIn(originalIn);
		}
	}
}
