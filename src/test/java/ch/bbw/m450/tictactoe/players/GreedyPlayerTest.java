package ch.bbw.m450.tictactoe.players;

import static ch.bbw.m450.tictactoe.TestBoards.boardFrom;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.bbw.m450.tictactoe.TestBoards;
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

	@Test
	void playsTopLeftOnAnEmptyBoard() {
		assertThat(player.play(TestBoards.emptyBoard(), Stone.CROSS)).isZero();
	}

	@Test
	void skipsOccupiedFields() {
		assertThat(player.play(boardFrom("XO.",
				"...",
				"..."), Stone.CROSS)).isEqualTo(2);
	}

	@Test
	void takesTheLastFreeField() {
		assertThat(player.play(boardFrom("XOX",
				"OXO",
				"OX."), Stone.CROSS)).isEqualTo(8);
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
