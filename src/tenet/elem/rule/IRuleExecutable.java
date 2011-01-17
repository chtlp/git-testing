package tenet.elem.rule;

/**
 * This Interface describes a object that can hold rules. No necessary of
 * executing rules itself, but rules must be executed by someone aggregated in
 * it.
 * 
 * @author Meilun sheng
 * 
 */
public interface IRuleExecutable {
	/**
	 * Add the rule to the last.
	 * 
	 * @param rule
	 * @return the index that the rule is placed
	 */
	public int addRule(Rule rule);

	/**
	 * Insert the rule at index. It might lead to rules behind this position
	 * move backwards.
	 * 
	 * @param rule
	 * @param index
	 * @return the index that the rule is placed
	 */
	public int addRule(Rule rule, int index);

	/**
	 * Delete the specific Rule
	 * 
	 * @param rule
	 * @return true if exists.
	 */
	public boolean deleteRule(Rule rule);

	/**
	 * Get the rule at index
	 * 
	 * @param index
	 * @return rule
	 */
	public Rule getRule(int index);
}
