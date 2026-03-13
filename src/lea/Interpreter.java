package lea;

import java.util.*;


import lea.Node.*;
import lea.Reporter.Phase;

public class Interpreter {

	private final Reporter reporter;
	private final Map<Identifier, Value> variables = new HashMap<>();

	public Interpreter(Reporter reporter) {
		this.reporter=reporter;
	}



	public void execute(Program program) {
		try {
			interpret(program.body());
		} catch (PanicException e) {
		} catch (BreakException e) {}
	}



	private void interpret(Instruction instruction) throws PanicException, BreakException {
		switch(instruction) {
		case Sequence s		-> interpret(s);
		case Assignment a	-> variables.put(a.lhs(), eval(a.rhs()));
		case Write w		-> interpret(w);
		case If i			-> interpret(i);
		case While w		-> interpret(w);
		case For f 			-> interpret(f);
		case ErrorNode e	-> throw error(e, "Le programme contient une erreur de syntaxe");
		case Break b 		-> throw errorbreak(b,"Code mort");
		}
	}

	private void interpret(Write w) throws PanicException, BreakException {
		Value value = eval(w.value());
		switch(value) {
		case Int i		-> System.out.print(i.value());
		case Bool b		-> System.out.print(b.value());
		}
		reporter.out(value);
		System.out.println();
	}

	private void interpret(Sequence sequence) throws PanicException, BreakException {
		for(var commande : sequence.commands()) 
			interpret(commande);
	}

	private void interpret(If i) throws PanicException, BreakException {
		if(evalAsBool(i.cond())) {
			interpret(i.bodyT());
		} else if(i.bodyF().isPresent()) {
			interpret(i.bodyF().get());
		}
	}

	private void interpret(While w) throws PanicException, BreakException {
		while(evalAsBool(w.cond())) {
			try{
				interpret(w.body());
			}
			catch(BreakException b ){
				break;
			}
			
			
		}
	}

	private void interpret(For f) throws PanicException, BreakException {
		int start = evalAsInt(f.start());
		int end = evalAsInt(f.end());
		if(start < end) {
			int step = f.step().isPresent() ? evalAsInt(f.step().get()) : 1;
			if(step <= 0) throw error(f.step().get(), "Boucle pour infinie");
			for(int i = start; i <= end; i+=step) {
				variables.put(f.id(), new Int(i));
				
				try{
				interpret(f.body());
				System.out.println(f.getClass());
			}
			catch(BreakException b ){
				break;
				
			}
			}
		} else {
			int step = f.step().isPresent() ? evalAsInt(f.step().get()) : -1;
			if(step >= 0) throw error(f.step().get(), "Boucle pour infinie");
			for(int i = start; i >= end; i+=step) {
				variables.put(f.id(), new Int(i));
				try{
				interpret(f.body());
			}
			catch(BreakException b ){
				
				break;
				
			}
			}
		}
	}




	private Value eval(Expression expression) throws PanicException {
		return switch(expression) {
		case Value l		-> l;
		case Identifier id 	-> eval(id);
		case Sum s			-> new Int(evalAsInt(s.left()) + evalAsInt(s.right()));
		case Difference d	-> new Int(evalAsInt(d.left()) - evalAsInt(d.right()));
		case Product p		-> new Int(evalAsInt(p.left()) * evalAsInt(p.right()));
		case Lower l		-> new Bool(evalAsInt(l.left()) < evalAsInt(l.right()));
		case Equal e 		-> new Bool(eval(e.left()).equals(eval(e.right())));
		case And a			-> new Bool(evalAsBool(a.left()) && evalAsBool(a.right()));
		case Or o 			-> new Bool(evalAsBool(o.left()) || evalAsBool(o.right()));
		case Inverse i		-> new Int(-evalAsInt(i.argument()));
		case Not n			-> new Bool(!evalAsBool(n.argument()));
		case ErrorNode e	-> throw error(e, "Le programme contient une erreur de syntaxe");
		};
	}

	private Value eval(Identifier id) throws PanicException {
		Value v = variables.get(id);
		if (v != null) return v;
		throw error(id, "Utilisation d'une variable pas initialisée");
	}

	private boolean evalAsBool(Expression expression) throws PanicException {
		return switch(eval(expression)) {
		case Bool b -> b.value();
		default -> throw error(expression, "Type (booléen)");
		};
	}

	private int evalAsInt(Expression expression) throws PanicException {
		return switch(eval(expression)) {
		case Int i -> i.value();
		default -> throw error(expression, "Type (entier)");
		};
	}



	private static class PanicException extends Exception {
		private static final long serialVersionUID = 1L;
		public PanicException(String message) {super(message);}
	}
	
	private PanicException error(Node n, String message) {
		reporter.error(Phase.RUNTIME, n, message);
		return new PanicException(message);
	}

	private static class BreakException extends Exception {
		private static final long serialVersionUID = 1L;
		public BreakException(String message) {super(message);}
	}
	
	private BreakException errorbreak(Node n, String message) {
		reporter.error(Phase.RUNTIME, n, message);
		return new BreakException(message);
	}

}
