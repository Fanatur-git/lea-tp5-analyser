package lea;

import java.util.*;
import lea.Reporter.Phase;
import lea.Node.*;

public final class Analyser {

    private final Reporter reporter;

    public Analyser(Reporter reporter) {
        this.reporter = reporter;
    }

    // Classe pour propager contexte + alive
    private static final class AnalyseResult {
        final Context context;
        final boolean alive;
        AnalyseResult(Context context, boolean alive) {
            this.context = context;
            this.alive = alive;
        }
    }

    public void analyse(Program program) {
        analyse(program, new Context(program));
    }

    private AnalyseResult analyse(Node node, Context context) {
        return switch (node) {
            case Program p      -> analyse(p.body(), context);
            case Sequence s     -> analyse(s, context);
            case Assignment a   -> new AnalyseResult(analyse(a, context), true);
            case Write w        -> new AnalyseResult(analyse(w.value(), context).context, true);
            case If i           -> analyse(i, context);
            case While w        -> analyse(w, context);
            case For f          -> analyse(f, context);
            case Break b        -> new AnalyseResult(context, false);
            case Value v        -> new AnalyseResult(context, true);
            case Identifier id  -> new AnalyseResult(analyse(id, context), true);
            case Sum s          -> new AnalyseResult(analyse(s.right(), analyse(s.left(), context).context).context, true);
            case Difference d   -> new AnalyseResult(analyse(d.right(), analyse(d.left(), context).context).context, true);
            case Product p      -> new AnalyseResult(analyse(p.right(), analyse(p.left(), context).context).context, true);
            case And a          -> new AnalyseResult(analyse(a.right(), analyse(a.left(), context).context).context, true);
            case Or o           -> new AnalyseResult(analyse(o.right(), analyse(o.left(), context).context).context, true);
            case Equal e        -> new AnalyseResult(analyse(e.right(), analyse(e.left(), context).context).context, true);
            case Lower l        -> new AnalyseResult(analyse(l.right(), analyse(l.left(), context).context).context, true);
            case Inverse i      -> new AnalyseResult(analyse(i.argument(), context).context, true);
            case Not n          -> new AnalyseResult(analyse(n.argument(), context).context, true);
            case ErrorNode e    -> new AnalyseResult(context, true);
        };
    }

    private AnalyseResult analyse(Sequence sequence, Context context) {
        boolean alive = true;
        for (var instr : sequence.commands()) {
            if (alive) {
                AnalyseResult res = analyse(instr, context);
                context = res.context;
                alive = res.alive;
            } else {
                context = error(instr, "Code mort", context);
            }
        }
        return new AnalyseResult(context, alive);
    }

    private Context analyse(Assignment assignment, Context context) {
        if (!context.declared.contains(assignment.lhs()))
            error(assignment.lhs(), "Variable non déclarée", context);
        Context cRhs = analyse(assignment.rhs(), context).context;
        return cRhs.withWritten(assignment.lhs());
    }

    private AnalyseResult analyse(If i, Context context) {
        AnalyseResult condRes = analyse(i.cond(), context);
        AnalyseResult thenRes = analyse(i.bodyT(), condRes.context);

        AnalyseResult elseRes;
        if (i.bodyF().isPresent()) {
            elseRes = analyse(i.bodyF().get(), condRes.context);
        } else {
            elseRes = new AnalyseResult(condRes.context, true);
        }

        Context merged = thenRes.context.merge(elseRes.context);
        boolean aliveAfter = thenRes.alive || elseRes.alive;

        return new AnalyseResult(merged, aliveAfter);
    }

    private AnalyseResult analyse(While w, Context context) {
        AnalyseResult condRes = analyse(w.cond(), context);
        AnalyseResult bodyRes = analyse(w.body(), condRes.context);
        Context merged = condRes.context.merge(bodyRes.context);
        return new AnalyseResult(merged, true); // flux actif après boucle
    }

    private AnalyseResult analyse(For f, Context context) {
        context = analyse(f.start(), context).context;
        context = analyse(f.end(), context).context;
        if (f.step().isPresent()) context = analyse(f.step().get(), context).context;

        Context contextBefore = context.withWritten(f.id());
        AnalyseResult bodyRes = analyse(f.body(), contextBefore);
        Context merged = contextBefore.merge(bodyRes.context);
        return new AnalyseResult(merged, true);
    }

    private Context analyse(Identifier id, Context context) {
        if (!context.declared.contains(id))
            error(id, "Variable non déclarée", context);
        else if (!context.written.contains(id))
            error(id, "Variable non initialisée", context);
        return context;
    }

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