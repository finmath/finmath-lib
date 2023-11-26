package net.finmath.util.config.nodes;

public class ValueNode implements Node {
	
	private final Object value;

	public ValueNode(Object value) {
		super();
		this.value = value;
	}

	public Object getValue() {
		return value;
	}
}