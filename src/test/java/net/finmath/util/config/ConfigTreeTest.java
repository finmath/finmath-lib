package net.finmath.util.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import net.finmath.util.config.nodes.SpecialNodes;

/**
 * Configuration Test
 * 
 * @author Christian Fries
 */
public class ConfigTreeTest {

	@Test
	public void test() {

		/**
		 * Create a list of map of configs (this is the config file - each map is a row, the keys are the columns)
		 */
		List<Object> prop1Values = List.of(0.5, 1.0, SpecialNodes.DEFAULT_VALUE);
		List<Object> prop2Values = List.of(1, 2, SpecialNodes.DEFAULT_VALUE);
		List<Object> prop3Values = List.of("a", "b", "c", SpecialNodes.DEFAULT_VALUE);

		Double valueForConfig = 0.0;
		List<Map<String, Object>> configs = new ArrayList<>();
		for(Object prop1Value : prop1Values) {
			for(Object prop2Value : prop2Values) {
				for(Object prop3Value : prop3Values) {
					configs.add(Map.of(
							"prop1", prop1Value,
							"prop2", prop2Value,
							"prop3", prop3Value,
							"value", valueForConfig
							));
					valueForConfig += 1.0;
				}
			}
		}

		System.out.println("Input Configurations");
		configs.forEach(System.out::println);
		System.out.println("_".repeat(79));

		// Build configTree			
		ConfigTree configTree = new ConfigTree(List.of("prop1", "prop2", "prop3"), configs);		

		// Fetch some stuff

		print(configTree, Map.of(
				"prop1", Double.valueOf(0.5),
				"prop2", Integer.valueOf(1),
				"prop3", String.valueOf("b")
				)
				,1.0);

		print(configTree, Map.of(
				"prop1", Double.valueOf(3),
				"prop2", Integer.valueOf(1),
				"prop3", String.valueOf("b")
				),
				25.0);

		print(configTree, Map.of(
				"prop1", Double.valueOf(0.5),
				"prop2", Integer.valueOf(1),
				"prop3", String.valueOf("a")
				),
				0.0);
	}

	private static void print(ConfigTree configTree, Map<String, Object> selector, Object expected) {
		Object value = configTree.getConfig(selector);
		System.out.print(value + "\tfor\t" + selector.toString());
		Assertions.assertEquals(expected, value);
		System.out.println("\tOK");
	}
}
