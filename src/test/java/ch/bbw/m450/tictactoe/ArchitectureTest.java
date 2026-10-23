package ch.bbw.m450.tictactoe;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;

/**
 * Architecture rules, checked by ArchUnit on the compiled production classes. Unlike the other
 * tests this does not run the code: it analyses which classes implement and access what.
 */
class ArchitectureTest {

	private static JavaClasses productionClasses;

	@BeforeAll
	static void importProductionClasses() {
		// The test classes live in the same packages (e.g. ...players.GreedyPlayerTest) and must
		// not be judged by rules meant for production code, hence DoNotIncludeTests.
		productionClasses = new ClassFileImporter()
				.withImportOption(new ImportOption.DoNotIncludeTests())
				.importPackages("ch.bbw.m450.tictactoe");
	}

	@Test
	void everyClassInThePlayersPackageIsAPlayer() {
		classes().that().resideInAPackage("..players..")
				.should().implement(TicTacToePlayer.class)
				.check(productionClasses);
	}

	/**
	 * Only the human player may talk to the keyboard. All other players must stay free of side
	 * effects, which is what lets tests play thousands of games against them.
	 */
	@Test
	void onlyTheHumanPlayerReadsStandardInput() {
		noClasses().that().resideInAPackage("..players..")
				.and().doNotHaveSimpleName("HumanPlayer")
				.should().accessField(System.class, "in")
				.check(productionClasses);
	}
}
