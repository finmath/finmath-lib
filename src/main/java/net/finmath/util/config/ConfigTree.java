package net.finmath.util.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import net.finmath.util.config.nodes.ConfigNode;
import net.finmath.util.config.nodes.Node;
import net.finmath.util.config.nodes.SpecialNodes;
import net.finmath.util.config.nodes.ValueNode;

/**
 * Config Tree: A tree based representation of configurations that can be selected with a key-value map describing the selector.
 * 
 * <h2>Selector &mapsto; Configuration</h2>
 *
 * <dl>
 * <dt>configuration</dt>
 * <dd>
 * A configuration is a value (Object) assigned to a selector (Map&lt;String, Object&gt;).
 * </dd>
 * 
 * <dt>selector</dt>
 * <dd>
 * A selector is a key-value map where certain properties (String) have certain values (Object).
 * Properties of the selector are checked in a fixed order,
 * such that a tree is formed, where each property corresponds to a level (depth) in the tree.
 * </dd>
 * 
 * <dt>default values</dt>
 * <dd>
 * Each level has a special branch for DEFAULT VALUES, if the given selector value does not match any value of other nodes.
 * </dd>
 * </dl>
 * 
 * <h2>Selector Key Order</h2>
 * 
 * The selector is matched against the configurtion list by checking the keys in a certain order. This oder does not matter if there is a unique value
 * for the given selector, but it may matter if default values have to be assign. See the following example.
 * 
 * <h2>Example</h2>
 * 
 * The configuration is determied by the value of two properties ("prop1" and "prop2") (the keys). The correspondig configuration value is
 * 
 * <p></p>

 * <table border="1">
 * <tr><th style="text-align: center;">prop1</th><th style="text-align: center;">prop2</th><th style="text-align: center;">value</th></tr>
 * <tr><td style="text-align: center;">A</td><td style="text-align: center;">1</td><td style="text-align: center;">42.0</td></tr>
 * <tr><td style="text-align: center;">A</td><td style="text-align: center;">2</td><td style="text-align: center;">3141</td></tr>
 * <tr><td style="text-align: center;">B</td><td style="text-align: center;">1</td><td style="text-align: center;">1</td></tr>
 * <tr><td style="text-align: center;">B</td><td style="text-align: center;">2</td><td style="text-align: center;">2</td></tr>
 * <tr><td style="text-align: center;">A</td><td style="text-align: center;">DEFAULT_VALUE</td><td style="text-align: center;">12</td></tr>
 * <tr><td style="text-align: center;">DEFAULT_VALUE</td><td style="text-align: center;">2</td><td style="text-align: center;">13</td></tr>
 * <tr><td style="text-align: center;">DEFAULT_VALUE</td><td style="text-align: center;">DEFAULT_VALUE</td><td style="text-align: center;">66</td></tr>
 * <caption>The list of maps that defines all configurations.</caption>
 * </table>
 * 
 * <p></p>
 * 
 * Initialising the class with this configuration we have:
 * <ul>
 * 	<li>
 * 		If the class is initialized with keyOder = { "prop1" , "prop2" }
 * 		<ul>
 * 			<li>The <i>selector</i> { "prop1" = "A", "prop2" = 2 } results in the value 3141.</li>
 * 			<li>The <i>selector</i> { "prop1" = "C", "prop2" = 2 } results in the value 13 (the default branch for prop1 is selected first, under that branch value 2 for "prop2" exists).</li>
 * 			<li>The <i>selector</i> { "prop1" = "C", "prop2" = 1 } results in the value 66 (the default branch for prop1 is selected first, under that branch no value 1 for "prop2" exists).</li>
 * 		</ul>
 * 	</li>
 * 	<li>
 * 		If the class is initialized with keyOder = { "prop2" , "prop1" }
 * 		<ul>
 * 			<li>The <i>selector</i> { "prop1" = "A", "prop2" = 2 } results in the value 3141.</li>
 * 			<li>The <i>selector</i> { "prop1" = "C", "prop2" = 2 } results in the value 13 (the branch "2" for prop2 is selected first, under that branch no value C for "prop1" exists).</li>
 * 			<li>The <i>selector</i> { "prop1" = "C", "prop2" = 1 } results in an exception that no configuration is found (the branch 1 for prop2 is selected first, under that branch no value C for "prop1" exists).</li>
 * 		</ul>
 * 	</li>
 * </ul>
 * 
 * @author Christian Fries
 */
public class ConfigTree {

	private final Node root;

	/**
	 * Construct the tree.
	 * 
	 * @param keyOrder Order in which the string keys of a selector define the levels of the tree.
	 * @param configs A list, where each element is map of keys to object. The key "value" is interpreted as the configuration value. All other keys are interpreted as configuration properties.
	 */
	public ConfigTree(List<String> keyOrder, List<Map<String, Object>> configs) {
		this.root = group(keyOrder, configs);
	}

	/**
	 * Get the configuration for a given specification of the properties (selector).
	 * 
	 * The configutation tree is traversed by selecting each route though the value of a specific key in the selector,
	 * until the lead node is reached. 
	 * If keys are missing in the selector of if values do not match a predefined route, a default route is used.
	 * 
	 * @param selector Maps the name (String) of a property to its value (Object).
	 * @return The configuration value for the given selector.
	 */
	public Object getConfig(Map<String, Object> selector) {
		Node node = this.root;

		// Traverse the tree where each route is selected though the value of a specific key in the selector. 
		while(node instanceof ConfigNode) {
			ConfigNode configNode = (ConfigNode)node;
			if(selector.containsKey(configNode.getKey()) && configNode.getValueToConfig().keySet().contains(selector.get(configNode.getKey()))) {
				node = configNode.getValueToConfig().get(selector.get(configNode.getKey()));
			}
			else {
				node = configNode.getValueToConfig().get(SpecialNodes.DEFAULT_VALUE);
				if(Objects.isNull(node)) {
					throw new IllegalArgumentException("Neither a value nor a default branch exists in the config tree for " + configNode.getKey() + " at the current location. " + selector);
				}
			}
		}

		// Having reached the value node, return it.
		if(node instanceof ValueNode) {
			ValueNode valueNode = (ValueNode)node;
			return valueNode.getValue();
		}
		else {
			throw new IllegalArgumentException("Unable to resolve configuration from the given properties. " + selector);
		}
	}

	/**
	 * Helper for the constructor. Recursive contruction of the tree.
	 * 
	 * @param keyOrder Given key order.
	 * @param configs List of configs.
	 * @return Node of the (sub-)tree for the given config key.
	 */
	private Node group(List<String> keyOrder, List<Map<String, Object>> configs) {
		if(keyOrder.size() > 0) {
			// Group all elements by the first key in keyOrder....
			String key = keyOrder.get(0);
			Map<Object, List<Map<String, Object>>> grouped = configs.stream().collect(Collectors.groupingBy(map -> map.get(key)));

			// ...call group (recursive) for all values below this key taking the remainder of keyOrder...
			List<String> keyOrderRemain = keyOrder.subList(1, keyOrder.size());
			Map<Object, Node> valueToConfig = grouped.entrySet().stream().collect(Collectors.toMap(
					Map.Entry::getKey, entry -> group(keyOrderRemain, entry.getValue())));

			// ...create a ConfigNode for this key.
			return new ConfigNode(key, valueToConfig);
		}
		else {
			// If no keys are left in key order, create the leaf node
			if(configs.size() == 1) {
				Map<String, Object> config = configs.get(0);
				Object value = config.get("value");
				return new ValueNode( value);
			}
			else {
				throw new IllegalArgumentException("Multiple configs for the same selector values. " + Arrays.deepToString(configs.toArray()));
			}
		}
	}	
}
