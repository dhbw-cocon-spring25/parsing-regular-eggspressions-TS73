package de.dhbw.mh.redeggs;

import static de.dhbw.mh.redeggs.CodePointRange.range;
import static de.dhbw.mh.redeggs.CodePointRange.single;

/**
 * A parser for regular expressions using recursive descent parsing.
 * This class is responsible for converting a regular expression string into a
 * tree representation of a {@link RegularEggspression}.
 */
public class RecursiveDescentRedeggsParser {

	/**
	 * The symbol factory used to create symbols for the regular expression.
	 */
	protected final SymbolFactory symbolFactory;

	public final static char END_CHAR = 0;
	public final static char EPS = 'ε';
	public final static char EMPTY_SET = '∅';
	public final static String NON_LITERALS = "|*()[]^-" + END_CHAR;

	/**
	 * Constructs a new {@code RecursiveDescentRedeggsParser} with the specified
	 * symbol factory.
	 *
	 * @param symbolFactory the factory used to create symbols for parsing
	 */
	public RecursiveDescentRedeggsParser(SymbolFactory symbolFactory) {
		this.symbolFactory = symbolFactory;
	}

	String input;
	int pos = 0;

	public char peek() throws RedeggsParseException {
		if (input.isEmpty()) {
			return END_CHAR;
		}
		char c = input.charAt(0);
		return c;
	}

	public char consume() throws RedeggsParseException {
		if (input.isEmpty()) {
			return END_CHAR;
		}
		pos++;

		char c = peek();
		input = input.substring(1);
		return c;
	}

	public void match(char c) throws RedeggsParseException {
		char consumed = consume();
		if (consumed != c) {
			if (consumed == END_CHAR) {
				throw new RedeggsParseException("Input ended unexpectedly, expected symbol '" + c + "' at position 5.",
						consumed);
			}
			throw new RedeggsParseException("Expected '" + c + "' but found '" + consumed + "'", 0);
		}
	}

	private static boolean isLiteral(char c) {
		return !NON_LITERALS.contains(String.valueOf(c));
	}

	// private void excpetion(char symbol) throws RedeggsParseException {
	// if (symbol == END_CHAR) {
	// throw new RedeggsParseException("", pos);
	// }
	// throw new RedeggsParseException("", pos);
	// }

	/**
	 * Parses a regular expression string into an abstract syntax tree (AST).
	 * 
	 * This class uses recursive descent parsing to convert a given regular
	 * expression into a tree structure that can be processed or compiled further.
	 * The AST nodes represent different components of the regex such as literals,
	 * operators, and groups.
	 * 
	 * @param regex the regular expression to parse
	 * @return the {@link RegularEggspression} representation of the parsed regex
	 * @throws RedeggsParseException if the parsing fails or the regex is invalid
	 */
	public RegularEggspression parse(String regex) throws RedeggsParseException {
		// Load the regex into the input string
		input = regex;
		pos = 0;

		RegularEggspression result = regex();

		if (!input.isEmpty()) {
			throw new RedeggsParseException("Unexpected symbol '" + consume() + "' at position " + pos + ".", 0);
		}

		return result;
	}

	public RegularEggspression regex() throws RedeggsParseException {
		System.out.println("Parsing regex: " + input);
		char c = peek();
		if (isLiteral(c) || c == '(' || c == '[') {
			RegularEggspression left = concat();
			RegularEggspression res = union(left);

			return res;
		}
		throw new RedeggsParseException("Expected a literal or '(' or '[' but found '" + c + "'", 0);
	}

	public RegularEggspression union(RegularEggspression left) throws RedeggsParseException {
		System.out.println("Parsing union: " + input);
		char c = peek();
		if (c == '|') {
			match('|');
			RegularEggspression concat_res = concat();
			RegularEggspression union_res = union(new RegularEggspression.Alternation(left, concat_res));
			return union_res;
		} else if (c == END_CHAR || c == ')') {
			return left;
		}
		throw new RedeggsParseException("Expected '|', ')' or '" + END_CHAR + "' but found '" + c + "'", 0);
	}

	public RegularEggspression concat() throws RedeggsParseException {
		System.out.println("Parsing concat: " + input);
		char c = peek();
		if (isLiteral(c) || c == '(' || c == '[') {
			RegularEggspression left = kleene();
			RegularEggspression res = suffix(left);
			return res;
		}

		throw new RedeggsParseException("Expected a literal, '(' or '[' but found '" + c + "'", 0);
	}

	public RegularEggspression suffix(RegularEggspression left) throws RedeggsParseException {
		System.out.println("Parsing suffix: " + input);
		char c = peek();
		if (isLiteral(c) || c == '(' || c == '[') {
			RegularEggspression kleene_res = kleene();
			RegularEggspression suffix_res = suffix(new RegularEggspression.Concatenation(left, kleene_res));
			return suffix_res;
		} else if (c == '|' || c == ')' || c == END_CHAR) {
			return left;
		}

		throw new RedeggsParseException(
				"Expected a literal, '(', '[', '|', '" + END_CHAR + "' or ')' but found '" + c + "'", 0);
	}

	public RegularEggspression kleene() throws RedeggsParseException {
		System.out.println("Parsing kleene: " + input);
		char c = peek();
		if (isLiteral(c) || c == '(' || c == '[') {
			RegularEggspression base_res = base();
			return star(base_res);
		}

		throw new RedeggsParseException("Expected a literal, '(' or '[' but found '" + c + "'", 0);
	}

	public RegularEggspression star(RegularEggspression base_res) throws RedeggsParseException {
		System.out.println("Parsing star: " + input);
		char c = peek();
		if (c == '*') {
			match('*');
			return new RegularEggspression.Star(base_res);
		} else if (isLiteral(c) || c == '(' || c == '[' || c == '|' || c == ')' || c == END_CHAR) {
			return base_res;
		}

		throw new RedeggsParseException(
				"Expected a '*', literal, '(', '[', '|', ')' or '" + END_CHAR + "' but found '" + c + "'", 0);
	}

	public RegularEggspression base() throws RedeggsParseException {
		System.out.println("Parsing base: " + input);
		char c = peek();
		if (isLiteral(c)) {
			char symbol = consume();

			if (symbol == EMPTY_SET) {
				return new RegularEggspression.EmptySet();
			} else if (symbol == EPS) {
				return new RegularEggspression.EmptyWord();
			}

			VirtualSymbol literal = symbolFactory.newSymbol().include(single(symbol)).andNothingElse();
			return new RegularEggspression.Literal(literal);
		} else if (c == '(') {
			match('(');
			RegularEggspression res = regex();

			match(')');
			return res;
		} else if (c == '[') {
			match('[');
			boolean inverted = carrot();

			SymbolFactory.Builder builder = symbolFactory.newSymbol();
			builder = inhalt(builder, inverted);
			builder = range_nt(builder, inverted);

			match(']');

			RegularEggspression res = new RegularEggspression.Literal(builder.andNothingElse());
			return res;
		}
		throw new RedeggsParseException("Expect a literal, '(' or '[' but found '" + c + "'", pos);
	}

	public boolean carrot() throws RedeggsParseException {
		char c = peek();
		if (c == '^') {
			match('^');
			return true;
		} else if (isLiteral(c)) {
			return false;
		}

		throw new RedeggsParseException("Expect a literal or '^' but found '" + c + "'", 0);
	}

	public SymbolFactory.Builder range_nt(SymbolFactory.Builder builder, boolean inverted)
			throws RedeggsParseException {
		char c = peek();

		if (isLiteral(c)) {
			builder = inhalt(builder, inverted);
			builder = range_nt(builder, inverted);
			return builder;
		} else if (c == ']') {
			return builder;
		}

		throw new RedeggsParseException("Expect a literal or ']'", 0);
	}

	public SymbolFactory.Builder inhalt(SymbolFactory.Builder builder, boolean inverted) throws RedeggsParseException {
		char c = peek();

		if (isLiteral(c)) {
			char first = consume();
			return remain(builder, inverted, first);
		}

		throw new RedeggsParseException("Expect a literal but found '" + c + "'", 0);
	}

	public SymbolFactory.Builder remain(SymbolFactory.Builder builder, boolean inverted, char first)
			throws RedeggsParseException {
		char c = peek();

		if (c == '-') {
			match('-');

			char second = consume();

			if (inverted) {
				builder.exclude(range(first, second));
			} else {
				builder.include(range(first, second));
			}

			return builder;
		} else {
			if (inverted) {
				builder.exclude(single(first));
			} else {
				builder.include(single(first));
			}
			return builder;
		}
	}
}
