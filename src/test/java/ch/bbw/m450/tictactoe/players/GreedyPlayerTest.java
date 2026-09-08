package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.TestBoards.boardFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ch.bbw.m450.tictactoe.TicTacToePlayer.Stone;

/**
 * Tests for {@link GreedyPlayer}, which always plays as far top-left as possible.
 */
class GreedyPlayerTest {

	/** Fixture: a fresh player before every test. */
	private GreedyPlayer player;

	@BeforeEach
	void setUp() {
		player = new GreedyPlayer();
	}

	/**
	 * The expected values cover both boundaries of the board: the first field (0) on an empty
	 * board and the last field (8) on an almost full one.
	 */
	@ParameterizedTest(name = "on [{0}][{1}][{2}] the greedy player picks {3}")
	@CsvSource({
			"..., ..., ..., 0",  // empty board -> very first field
			"X.., ..., ..., 1",  // first field taken
			"XO., ..., ..., 2",  // first two fields taken
			"XOX, ..., ..., 3",  // first row full -> continues in the second row
			"XOX, OXO, OX., 8",  // only the last field is left
	})
	void picksTheFirstFreeField(String top, String middle, String bottom, int expected) {
		assertThat(player.play(boardFrom(top, middle, bottom), Stone.CROSS)).isEqualTo(expected);
	}

	@ParameterizedTest(name = "the choice does not depend on the own color ({0})")
	@CsvSource({"CROSS", "CIRCLE"})
	void ignoresTheOwnColor(Stone colorToPlay) {
		assertThat(player.play(boardFrom("XO.",
				"...",
				"..."), colorToPlay)).isEqualTo(2);
	}

	@Test
	void throwsWhenTheBoardIsFull() {
		assertThatThrownBy(() -> player.play(boardFrom("XOX",
				"OXO",
				"OXO"), Stone.CROSS))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("cannot play at all");
	}
}
