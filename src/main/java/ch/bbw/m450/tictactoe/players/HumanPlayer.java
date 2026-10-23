package ch.bbw.m450.tictactoe.players;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import ch.bbw.m450.tictactoe.TicTacToeMain;
import ch.bbw.m450.tictactoe.TicTacToePlayer;

/**
 * Simple human-player taking input from stdin.
 */
// The console IS this program's user interface (and what the E2E tests assert on), not
// diagnostic logging. A logger would be the wrong tool, so S106 is suppressed on purpose.
@SuppressWarnings("java:S106")
public class HumanPlayer implements TicTacToePlayer {

	// Shared across ALL HumanPlayer instances (not a per-instance field), keyed by the current
	// System.in reference. Two reasons a single, process-wide Scanner is needed instead of one per
	// instance:
	//
	// 1. TicTacToeMain.play() can be given two SEPARATE HumanPlayer instances (two people sharing
	//    one keyboard). Even with one Scanner per instance, both Scanners still wrap the same
	//    System.in -- Scanner reads ahead into its own private buffer, so whichever instance reads
	//    first can silently swallow input meant for the other instance's next turn.
	// 2. Tests swap System.in per test method (JUnit Pioneer's @StdIo). Comparing the cached
	//    Scanner's remembered stream against the CURRENT System.in, and rebuilding it when they
	//    differ, means each test still gets its own fresh Scanner over its own simulated input,
	//    while a real game -- where System.in never changes -- reuses a single Scanner for its
	//    whole lifetime, exactly like a plain static field would.
	private static Scanner scanner;
	private static InputStream scannerSource;

	private static synchronized Scanner sharedScanner() {
		if (scanner == null || scannerSource != System.in) {
			scannerSource = System.in;
			scanner = new Scanner(scannerSource, StandardCharsets.UTF_8);
		}
		return scanner;
	}

	@Override
	public int play(Stone[] board, Stone colorToPlay) {
		while (true) {
			System.out.println(TicTacToeMain.toString(board) + "where to to put the next " + colorToPlay + "? (0-8): ");
			var line = sharedScanner().nextLine().strip();
			try {
				return Integer.parseInt(line);
			} catch (NumberFormatException _) {
				// Re-prompt instead of letting a bad keystroke crash the whole game. Positions
				// that parse fine but are out of range or already occupied are a different,
				// deliberate concern -- TicTacToeMain.play() already validates and rejects those.
				System.out.println("'" + line + "' is not a valid number -- please enter a value between 0 and 8.");
			}
		}
	}
}
