package net.finmath.util.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.finmath.util.config.nodes.ConfigNode;
import net.finmath.util.config.nodes.Node;
import net.finmath.util.config.nodes.SpecialNodes;
import net.finmath.util.config.nodes.ValueNode;

/**
 * Config Tree: A tree based representation of configurations that can be selected with a key-value map describing the selector.
 * 
 * A configuration is a value (Object) assigned to a selector (Map&lt;String, Object&gt;).
 * A selector is a key-value map where certain properties (String) have certain values (Object).
 * Properties of the selector are checked in a fixed order,
 * such that a tree is formed, where each property corresponds to a level (depth) in the tree.
 * Each level has a special branch for DEFAULT VALUES, if the selector value does not match any value of other nodes.
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
			}
		}

		// Having reached the value node, return it.
		if(node instanceof ValueNode) {
			ValueNode valueNode = (ValueNode)node;
			return valueNode.getValue();
		}
		else {
			throw new IllegalArgumentException("Unable to resolve configuration from the given properties.");
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
