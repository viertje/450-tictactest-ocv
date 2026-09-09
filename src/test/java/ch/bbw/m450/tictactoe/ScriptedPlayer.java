package ch.bbw.m450.tictactoe;

/**
 * Test double (stub) for a {@link TicTacToePlayer}.
 * <p>
 * Plays a fixed sequence of positions instead of thinking or reading from stdin. This makes it
 * possible to test {@link TicTacToeMain#play} with an exactly known game, including invalid moves.
 */
final class ScriptedPlayer implements TicTacToePlayer {

	private final int[] moves;

	private int nextMove = 0;

	/**
	 * @param moves the positions to play, in order
	 */
	ScriptedPlayer(int... moves) {
		this.moves = moves;
	}

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		if (nextMove >= moves.length) {
			throw new IllegalStateException("scripted player was asked for move " + (nextMove + 1)
					+ ", but only " + moves.length + " were scripted");
		}
		return moves[nextMove++];
	}
}
