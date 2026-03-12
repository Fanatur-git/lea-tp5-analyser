package lea;

import java.util.*;

import lea.Reporter.Phase;
import lea.Node.*;

public final class Analyser {

	private final Reporter reporter;

	public Analyser(Reporter reporter) {
		this.reporter = reporter;
	}

	public void analyse(Program program) {
		analyse(program, new Context(program));
	}

	private Context analyse(Node node, Context context) {
		return switch(node) {
		case Program p			-> analyse(p.body(), context);

		case Sequence s			-> analyse(s, context);
		case Assignment a		-> analyse(a, context);
		case Write w			-> analyse(w.value(), context);
		case If i				-> analyse(i, context); 
		case While w			-> analyse(w, context); 
		case For f				-> analyse(f, context);
		 

		case Value v			-> context;
		case Identifier id		-> analyse(id, context);
		case Sum s				-> analyse(s.right(), analyse(s.left(), context));
		case Difference d		-> analyse(d.right(), analyse(d.left(), context));
		case Product p			-> analyse(p.right(), analyse(p.left(), context));
		case And a				-> analyse(a.right(), analyse(a.left(), context));
		case Or o				-> analyse(o.right(), analyse(o.left(), context));
		case Equal e			-> analyse(e.right(), analyse(e.left(), context));
		case Lower l			-> analyse(l.right(), analyse(l.left(), context));
		case Inverse i			-> analyse(i.argument(), context);
		case Not n				-> analyse(n.argument(), context);

		case ErrorNode e		-> context;
		};
	}

	private Context analyse(Sequence sequence, Context context) {
		for(var commande : sequence.commands()) {
			context = analyse(commande, context);
		}
		return context;
	}

	private Context analyse(Assignment assignment, Context context) {
		if (!context.declared.contains(assignment.lhs()))	
			error(assignment.lhs(), "Variable non déclarée", context);
		Context cRhs = analyse(assignment.rhs(), context);
		return cRhs.withWritten(assignment.lhs());
	}

	private Context analyse(If i, Context context) {
		Context cCond  = analyse(i.cond(), context);
		Context cTrue  = analyse(i.bodyT(), cCond);
		if(i.bodyF().isEmpty())
			return cCond.merge(cTrue);
		Context cFalse = analyse(i.bodyF().get(), cCond);
		return cTrue.merge(cFalse);
	}

	private Context analyse(While w, Context context) {
		Context cCond = analyse(w.cond(), context);
		Context cBody = analyse(w.body(), cCond);
		return cCond.merge(cBody);
	}

	private Context analyse(For f, Context context) {
		context = analyse(f.start(), context);
		context = analyse(f.end(), context);
		if(f.step().isPresent()) context = analyse(f.step().get(), context);
		Context cBefore = context.withWritten(f.id());
		Context cBody = analyse(f.body(), cBefore);
		return cBefore.merge(cBody);
	}

	private Context analyse(Identifier id, Context context) {
		if (!context.declared.contains(id))	error(id, "Variable non déclarée", context);
		else if (!context.written.contains(id))	error(id, "Variable non initialisée", context);
		return context;
	}

	
	
	
	
	/**
	 * Gestion des erreurs
	 * @param n
	 * @param message
	 * @param context
	 * @return
	 */
	private Context error(Node n, String message, Context context) {
		reporter.error(Phase.STATIC, n, message);
		return context;
	}

	private static final class Context {

		final Set<Identifier> declared;
		final Set<Identifier> written;

		public Context(Program program) {
			declared = Set.copyOf(program.declared());
			written = Set.of();
		}

		private Context(Set<Identifier> declared, Set<Identifier> written) {
			this.declared = Set.copyOf(declared);
			this.written = Set.copyOf(written);
		}

		public Context withWritten(Identifier id) { 
			var writ = new HashSet<>(written); 
			writ.add(id); 
			return new Context(declared, writ); 
		}

		public Context merge(Context other) {
			var writ = new HashSet<>(written);
			writ.retainAll(other.written);
			return new Context(declared, writ);
		}

	}

}
