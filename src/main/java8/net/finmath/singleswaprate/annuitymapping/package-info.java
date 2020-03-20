/**
 * Classes providing options for the annuity mapping function. These replace the annuity, which is dependent on bonds of multiple maturities, with a function that solely
 * depends on a single swap rate. Thus allowing to use vanilla models where otherwise term structure models would be necessary.
 *
 * @author Christian Fries
 * @author Roland Bachl
 */
package net.finmath.singleswaprate.annuitymapping;
