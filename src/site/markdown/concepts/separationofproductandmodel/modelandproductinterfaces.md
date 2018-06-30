# Model and Product Interfaces

The library structures the problem of valuation of financial products with respect to several aspects:

- asset classes (single asset models, interest rate term structure models, etc.)
- numerical methods (Monte-Carlo, finite difference, Fourier transform, analytic formulas)
- specific modelling assumptions 

## Overview

|Type                        |Model                                              |Product                            | 
|:---------------------------|:--------------------------------------------------|:----------------------------------| 
|Base type                   |``ModelInterface``                                 |``ProductInterface``               |
|Analytic valuation          |``AnalyticModelInterface``                         |``AbstractAnalyticProduct``        |
|Monte Carlo / Equity        |``AssetModelMonteCarloSimulationInterface``        |``AbstractAssetMonteCarloProduct`` |
|Monte Carlo / Interest Rates|``TermStructureModelMonteCarloSimulationInterface``|``AbstractLIBORMonteCarloProduct`` |
|Fourtier Transform / 1D     |``ProcessCharacteristicFunctionInterface``         |``AbstractProductFourierTransform``|
|Finite Difference / 1D      |``FiniteDifference1DModel``                        |``FiniteDifference1DProduct``      |

## Models

ModelInterface

- marker interface

### Monte Carlo

MonteCarloSimulationInterface

- provides getTimeDiscretization, getNumberOfPaths, getRandomVariableForConstant, getMonteCarloWeights

#### Equity

AssetModelMonteCarloSimulationInterface extends MonteCarloSimulationInterface
- provides getNumeraire, getAsset

##### Implementation

MonteCarloAssetModel implements AssetModelMonteCarloSimulationInterface
MonteCarloMertonModel implements AssetModelMonteCarloSimulationInterface
HybridAssetLIBORModelMonteCarloSimulationInterface extends LIBORModelMonteCarloSimulationInterface, AssetModelMonteCarloSimulationInterface

#### Interest Rates

TermStructureModelMonteCarloSimulationInterface extends MonteCarloSimulationInterface

- provides getNumeraire, getLIBOR(double, double, double)


### Fourier Transform

ProcessCharacteristicFunctionInterface

``HestonModel implements Model<HestonModelDescriptor>, ProcessCharacteristicFunctionInterface``


### Example:

``class HestonMonteCarloModel extends MonteCarloAssetModel implements Model<HestonModelDescriptor>``

- A HestonModel implementing ``AssetModelMonteCarloSimulationInterface`` and ``Model<HestonModelDescriptor>``
- The model can be used for valuation via ``AssetModelMonteCarloSimulationInterface``. The model is build via a ``HestonModelDescriptor``.


## Products

ProductInterface

- provides ``getValue(Model<?> model)``

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

