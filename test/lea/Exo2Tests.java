package lea;

import org.junit.jupiter.api.Test;
import lea.Reporter.Phase;

/**
 * TDD Tests for Exercise 2: Break and Dead Code Detection.
 */
public final class Exo2Tests {

	/* ==================
	 * === Question 2a ==
	 * ================== */

	@Test
	void break_syntax_is_accepted() {
		new LeaAsserts("""
				algorithme
				début
					tant que vrai faire
						interrompre;
					fin tant que
				fin
				""").assertHasNoErrorAt(Phase.PARSER);
	}

	@Test
	void break_actually_stops_loop() {
		new LeaAsserts("""
				algorithme
				variables x;
				début
					x <- 0;
					tant que x < 10 faire
						x <- x + 1;
						si x = 3 alors
							interrompre;
						fin si
					fin tant que
					écrire(x);
				fin
				""").assertOutputs(new Node.Int(3));
	}

	/* ==================
	 * === Question 2b ==
	 * ================== */


	@Test
	void deadCode_while() {
		new LeaAsserts("""
				  algorithme
				  variables
				    x;
				  début
				    tant que x < 10 faire
				      interrompre;
				      écrire(1);
				    fin tant que
				    écrire(2);
				  fin
				""").assertOutputs(new Node.Int(2));
	}


	@Test
	void deadCode_cond_true_branch() {
		new LeaAsserts("""
				  algorithme
				  variables
				    x;
				  début
				    x <- 0;
				    tant que x < 10 faire
				      si x < 5 alors
				        interrompre;
				        écrire(1);
				      sinon
				        écrire(2);
				      fin si
				      écrire(3);
				    fin tant que
				       écrire(4);
				  fin
				""").assertOutputs(new Node.Int(4));
	}


	@Test
	void deadCode_both_branches() {
		new LeaAsserts("""
				  algorithme
				  variables
				    x;
				  début
				    x <- 0;
				    tant que x < 10 faire
				      si x < 5 alors
				        interrompre;
				      sinon
				        interrompre;
				      fin si
				      écrire(1);
				    fin tant que
				    écrire(2);
				  fin
				""").assertOutputs(new Node.Int(2));
	}


	/* ==================
	 * === Question 2c ==
	 * ================== */

	@Test
	void deadCode_simple_sequence() {
		new LeaAsserts("""
				algorithme
				début
					tant que vrai faire
						interrompre;
						écrire(999); // Mort
					fin tant que
				fin
				""").assertHasErrorContaining(Phase.STATIC, "Code mort");
	}

	@Test
	void deadCode_if_only_one_branch_breaks() {
		new LeaAsserts("""
				algorithme
				début
					tant que vrai faire
						si vrai alors
							interrompre;
						sinon
							écrire(2);
						fin si
						écrire(3);
					fin tant que
				fin
				""").assertHasNoErrorAt(Phase.STATIC);
	}

	@Test
	void deadCode_if_both_branches_break() {
		new LeaAsserts("""
				algorithme
				début
					tant que vrai faire
						si vrai alors
							interrompre;
						sinon
							interrompre;
						fin si
						écrire(3);
					fin tant que
				fin
				""").assertHasErrorContaining(Phase.STATIC, "Code mort");
	}

	@Test
	void deadCode_after_loop_is_still_reachable() {
		new LeaAsserts("""
				algorithme
				début
					tant que faux faire
						interrompre;
					fin tant que
					écrire(1); // Atteignable !
				fin
				""").assertHasNoErrorAt(Phase.STATIC);
	}

}