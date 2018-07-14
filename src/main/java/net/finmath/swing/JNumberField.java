/*
 * Created on 09.08.2005
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.ParseException;

import javax.swing.JTextField;

import net.finmath.time.TimeDiscretizationInterface;

/**
 * A Java swing bean to represent a number field in a GUI. Features admissible values.
 *
 * @author Christian Fries
 */
public class JNumberField extends JTextField implements ActionListener {

	private static final long serialVersionUID = -138039675088007707L;

	Number value = new Double(0.0);
	DecimalFormat formatter = new DecimalFormat("0.000");

	// Guards access to read or write of the text field
	Object updateLock = new Object();

	double		preferedValueIncrement = 0.0;
	double[]	admissibleValues = null;
	double lowerBound = -Double.MAX_VALUE;
	double upperBound = Double.MAX_VALUE;

	public JNumberField() {
		super();
		this.addActionListener(this);
	}

	public JNumberField(double value, String format, ActionListener actionListener) {
		super();
		formatter = new DecimalFormat(format);
		this.addActionListener(actionListener);
		this.addActionListener(this);
		setValue(value);
	}

	public JNumberField(double value, DecimalFormat format, ActionListener actionListener) {
		super();
		formatter = format;
		this.addActionListener(actionListener);
		this.addActionListener(this);
		setValue(value);
	}

	public JNumberField(String format) {
		super(format);
		formatter = new DecimalFormat(format);
		this.addActionListener(this);
		setValue(0.0);
	}

	public Number getValue() {
		parseField();
		updateData();
		return value;
	}

	public void setValue(double value) {
		this.value = value;
		updateData();
	}

	public void setFromat(String format) {
		formatter = new DecimalFormat(format);
		updateData();
	}

	public void setRange(double lowerBound, double upperBound) {
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		updateData();
	}

	public void setAdmissibleValues(double[] admissibleValues) {
		this.admissibleValues = admissibleValues;
	}

	public void setAdmissibleValues(TimeDiscretizationInterface timeDiscretization) {
		this.admissibleValues = new double[timeDiscretization.getNumberOfTimeSteps()+1];
		for(int i=0; i<admissibleValues.length; i++) {
			this.admissibleValues[i] = timeDiscretization.getTime(i);
		}
	}

	public double getPreferedValueIncrement() {
		return preferedValueIncrement;
	}

	public void setPreferedValueIncrement(double preferedValueIncrement) {
		this.preferedValueIncrement = preferedValueIncrement;
	}

	public void add(double increment) {
		setValue(getDoubleValue() + increment);
	}

	public void addToAdmissibleValueIndex(int increment) {
		if(admissibleValues != null) {
			int index = getAdmissibleValueIndex();
			if(index < 0) {
				return;	// Admissible values not set
			}

			index = Math.max(0,Math.min(index + increment, admissibleValues.length-1));
			value = admissibleValues[index];
		}
		else {
			add(increment * preferedValueIncrement);
		}

		updateData();
	}

	public double getDoubleValue() {
		return getValue().doubleValue();
	}

	public int getIntValue() {
		return getValue().intValue();
	}

	public void actionPerformed(ActionEvent arg0) {
		parseField();
		updateData();
	}

	private void parseField() {
		Double valueNumber = Double.NaN;
		synchronized (updateLock) {
			String valueText = this.getText();

			if(valueText != null) {
				try {
					valueNumber = formatter.parse(this.getText()).doubleValue();
				} catch (ParseException e) {}
			}
		}

		setValue(valueNumber);
	}

	private void updateData() {
		if(value == null) {
			parseField();
		}
		synchronized (updateLock) {

			// Constrain to admissibleValues
			int index = getAdmissibleValueIndex();
			if(index >= 0) {
				value = admissibleValues[index];
			}

			// Apply bounds
			this.value = new Double(Math.min(Math.max(lowerBound,value.doubleValue()),upperBound));

			// Write and resize field
			this.setText(formatter.format(value));

			if(lowerBound != -Double.MAX_VALUE && upperBound != Double.MAX_VALUE) {
				this.setColumns(1+Math.max(formatter.format(lowerBound).length(), formatter.format(upperBound).length()));
			} else {
				this.setColumns(1+this.getText().length());
			}

		}
	}

	private int getAdmissibleValueIndex() {
		// Constrain to admissibleValues
		if(this.admissibleValues != null && admissibleValues.length > 0) {
			int index = java.util.Arrays.binarySearch(admissibleValues, value.doubleValue());
			if(index < 0) {
				index = -index-1;
			}
			if(index > admissibleValues.length) {
				index--;
			}
			return index;
		} else {
			return -1;
		}
	}
}

