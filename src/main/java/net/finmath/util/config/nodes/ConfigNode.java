package net.finmath.util.config.nodes;

import java.util.Map;

public class ConfigNode implements Node {
	
	private final String key;
	private final Map<Object, Node> valueToConfig;
	
	public ConfigNode(String key, Map<Object, Node> valueToConfig) {
		super();
		this.key = key;
		this.valueToConfig = valueToConfig;
	}

	public String getKey() {
		return key;
	}

	public Map<Object, Node> getValueToConfig() {
		return valueToConfig;
	}
}