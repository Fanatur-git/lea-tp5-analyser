package lea;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public sealed interface Node {

	public record Program(Set<Identifier> declared, Instruction body)		implements	Node {}

	public sealed interface Instruction 									extends 	Node {}
	public record Sequence(List<Instruction> commands)						implements	Instruction {}
	public record Assignment(Identifier lhs, Expression rhs)				implements	Instruction {}
	public record Write(Expression value)									implements	Instruction {}
	public record If(Expression cond, 
			Instruction bodyT, Optional<Instruction> bodyF)					implements	Instruction {}
	public record While(Expression cond, Instruction body)					implements	Instruction {}
	public record For(Identifier id, Expression start, Expression end,
			Optional<Expression> step, Instruction body)					implements	Instruction {}
	public record Break()													implements	Instruction {}	

	public sealed interface Expression										extends 	Node {}
	public record Identifier(String text)									implements	Expression {}
	public record Sum(Expression left, Expression right)					implements	Expression {}
	public record Difference(Expression left, Expression right)				implements	Expression {}
	public record Product(Expression left, Expression right)				implements	Expression {}
	public record And(Expression left, Expression right)					implements	Expression {}
	public record Or(Expression left, Expression right)						implements	Expression {}
	public record Equal(Expression left, Expression right)					implements	Expression {}
	public record Lower(Expression left, Expression right)					implements	Expression {}
	public record Inverse(Expression argument)								implements	Expression {}
	public record Not(Expression argument)									implements	Expression {}

	public sealed interface Value											extends		Expression {}
	public record Bool(boolean value)										implements	Value {}
	public record Int(int value)											implements	Value {}

	public record ErrorNode()												implements	Instruction, Expression{}

}
