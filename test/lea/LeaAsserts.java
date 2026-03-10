package lea;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import java_cup.runtime.Symbol;
import lea.Node.*;
import lea.Reporter.Phase;

public class LeaAsserts {

	private final Reporter reporter = new Reporter();
	private final List<Symbol> tokens = new ArrayList<Symbol>();

	public LeaAsserts(String source) {
		try(Reader reader = new StringReader(source)) {
			var lexer = new Lexer(reader, new Reporter());
			while (!lexer.yyatEOF()) {
				Symbol token = lexer.next_token();
				if(token.sym != Terminal.EOF)
					tokens.add(token);
			}
		} catch (Exception e) {
			fail(e);
		}
		PrintStream originalOut = System.out;
	    System.setOut(new PrintStream(OutputStream.nullOutputStream()));
		try(Reader reader = new StringReader(source)) {
			var lexer = new Lexer(reader, reporter);
			var parser = new Parser(lexer,reporter);
			var analyser = new Analyser(reporter);
			var interpreter = new Interpreter(reporter);
			var program = parser.parseProgram();
			analyser.analyse(program);
			interpreter.execute(program);
		} catch (Exception e) {
			fail(e);
		} finally {
	        System.setOut(originalOut);
		}
	}

	private List<String> getErrors(Phase phase, String fragment) {
		return reporter
				.getErrors()
				.stream()
				.filter(d -> d.phase() == phase)
				.map(d->d.message())
				.filter(m -> m.contains(fragment))
				.toList();
	}

	public LeaAsserts assertHasNoErrorAt(Phase phase) {
		List<String> errors = getErrors(phase, "");
		assertTrue(errors.isEmpty(), () -> phase +  " errors:\n" + String.join("\n", errors));
		return this;
	}

	public LeaAsserts assertHasNoError() {
		assertHasNoErrorAt(Phase.LEXER);
		assertHasNoErrorAt(Phase.PARSER);
		assertHasNoErrorAt(Phase.STATIC);
		assertHasNoErrorAt(Phase.RUNTIME);
		return this;
	}
	
	public LeaAsserts assertHasErrorContaining(Phase phase, String fragment) {
		List<String> errors = getErrors(phase, fragment);
		assertTrue(!errors.isEmpty(), () -> "Expected " + phase + " error containing: \"" + fragment + "\"\nGot:\n" + String.join("\n", errors));
		return this;
	}

	public LeaAsserts assertOutputs(Value... expected) {
		var output = reporter.getOutput();
		assertEquals(expected.length, output.size(), "Output count mismatch: " + output);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], output.get(i), "Output mismatch at index " + i);
		}
		return this;
	}

	public LeaAsserts assertMatches(int... expected) {
		assertEquals(expected.length, tokens.size(), "Token count mismatch: " + tokens);
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], tokens.get(i).sym, "Token mismatch at index " + i);
		}
		return this;
	}

}
