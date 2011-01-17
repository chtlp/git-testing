package tenet.elem.rule;

/**
 * This interface is used to build a rule by parsing a command-line
 * 
 * @author Meilun Sheng
 * 
 */
public interface IRuleBuilder {
	public Rule build(String cmd) throws RuleFormatException;
}
