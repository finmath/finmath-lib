/**
 * Integrated Assessment Models.
 * 
 * Our convention here is that all models return annualized quantities, if quantities are related to time periods.
 * So if X denotes an annualized quantity like the GDP and emission for the period \( (t_{i},t_{i+1}) \), then the total quantity is
 * \[
 * 	X \cdot yearFraction(t_{i},t_{i+1})
 * \]
 *
 * @author Christian Fries
 * @author Lennart Quante
 */
package net.finmath.climate.models;
