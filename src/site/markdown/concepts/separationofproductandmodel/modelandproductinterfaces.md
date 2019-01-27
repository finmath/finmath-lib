# Model and Product Interfaces

The library structures the problem of valuation of financial products with respect to several aspects:

- asset classes (single asset models, interest rate term structure models, etc.)
- numerical methods (Monte-Carlo, finite difference, Fourier transform, analytic formulas)
- specific modeling assumptions 

## Overview

|Type                        |Model                                              |Product                            | 
|:---------------------------|:--------------------------------------------------|:----------------------------------| 
|Base type                   |``Model``                                          |``Product``                         |
|Analytic valuation          |``AnalyticModel``                                   |``AnalyticProduct``                 |
|Monte Carlo                 |``MonteCarloSimulationModel``                        |``MonteCarloProduct``                 |
|Monte Carlo / Equity        |``AssetModelMonteCarloSimulationModel``              |``AssetMonteCarloProduct`` |
|Monte Carlo / Interest Rates|``TermStructureMonteCarloSimulationModel``           |``TermStructureMonteCarloProduct`` |
|Monte Carlo / Interest Rates / Discrete Forward Rates|``LIBORMonteCarloSimulationModel``           |``TermStructureMonteCarloProduct`` |
|Fourier Transform / 1D      |``CharacteristicFunctionModel``                     |``FourierTransformProduct``|
|Finite Difference / 1D      |``FiniteDifference1DModel``                        |``FiniteDifference1DProduct``      |

## Models

`Model` (interface)

- marker interface

### Monte Carlo

`MonteCarloSimulationModel`

- provides getTimeDiscretization, getNumberOfPaths, getRandomVariableForConstant, getMonteCarloWeights

#### Equity

`AssetModelMonteCarloSimulationMode extends MonteCarloSimulationModel`

- provides getNumeraire, getAsset

##### Implementation and Extensions

MonteCarloAssetModel implements AssetModelMonteCarloSimulationMode
MonteCarloMertonModel implements AssetModelMonteCarloSimulationMode
HybridAssetLIBORModelMonteCarloSimulationModel extends LIBORMonteCarloSimulationModel, AssetModelMonteCarloSimulationMode

#### Interest Rates

`TermStructureMonteCarloSimulationModel extends MonteCarloSimulationModel`

- provides getNumeraire, getLIBOR(double, double, double)


### Fourier Transform (`net.finmath.fouriermethod`)

`CharacteristicFunctionModel`

- provides apply(double) returning a `CharacteristicFunction`

##### Implementation and Extensions

- ``HestonModel implements CharacteristicFunctionModel``


### Example:

``class HestonMonteCarloModel extends MonteCarloAssetModel implements Model<HestonModelDescriptor>``

- A HestonModel implementing ``AssetModelMonteCarloSimulationInterface`` and ``Model<HestonModelDescriptor>``
- The model can be used for valuation via ``AssetModelMonteCarloSimulationInterface``. The model is build via a ``HestonModelDescriptor``.


## Products

`Product` (interface)

- provides `getValue(Model<?> model)`.
  Objects implementing the getValue method should provide a double-dispatch on the model argument, i.e., casting to suitable models.

### Monte Carlo

AbstractMonteCarloProduct	``AbstractMonteCarloProduct implements ProductInterface``

- product which can be values by a ``MonteCarloSimulationInterface``
- manages currency, otherwise nothing
- looks more like a marker.

#### Monte Carlo / Equity

AbstractAssetMonteCarloProduct	``AbstractAssetMonteCarloProduct extends AbstractMonteCarloProduct``

- product can be valued by a ``AssetModelMonteCarloSimulationInterface``
- the getValue with ``MonteCarloSimulationInterface`` performs a typecheck and routes to the above.


``AssetModelMonteCarloSimulationInterface`` &rarr; ``MonteCarloSimulationInterface``
``AbstractAssetMonteCarloProduct`` &rarr; ``AbstractMonteCarloProduct`` &rarr; ``ProductInterface``

#### Monte Carlo / Interest Rates

``TermStructureModelMonteCarloSimulationInterface`` &rarr; ``MonteCarloSimulationInterface``
``AbstractLIBORMonteCarloProduct`` &rarr; ``AbstractMonteCarloProduct`` &rarr; ``ProductInterface``

### Fourier Transform
``AbstractProductFourierTransform implements CharacteristicFunctionInterface``


### Analytic

``AbstractAnalyticProduct`` &rarr; ``AnalyticProductInterface`` &rarr; ``ProductInterface``



Implementations: ``Swaps``

Swap extends AbstractAnalyticProduct implements AnalyticProductInterface (NOTE: implements is superflouus)
Swap extends AbstractLIBORMonteCarloProduct

Implementations: European Equity Options:

``EuropeanOption extends AbstractProductFourierTransform implements ProductInterface``

``EuropeanOption extends AbstractAssetMonteCarloProduct<SingleAssetEuropeanOptionProductDescriptor> implements ProductInterface``



## UML Diagrams (selected classes)

![UML Diagrams for model and product interfaces](modelandproductinterfaces.svg)

