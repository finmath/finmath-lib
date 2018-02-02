# Separation of Product and Model and Numerical Method
02.02.2017. Work in progress. Will appear in a future version (4.x).

## Introduction
The valuation library proposes the separation of financial products and models and numerical methods (as suggested in ISBN 0-470-04722-4).

The core concept here is to provide data, products, models and implementations independent of each other. For this the real of model and products is decomposed into the following components:

*product descriptors*: A product descriptor is a class representing a financial product. It is a model independent representation comparable to a POJO. There should be factories creating product descriptor out of FpML, XML or Key-Value representations and a product descriptor should be able to serialize itself into these formats.

*model descriptors*: A model descriptor is a class representing a financial model. It is independent of a specific numerical method. An example is a model descriptor for the equity Heston model, without specifying if the model is implemented as a PDE, Monte-Carlo simulation or Fourier transform.

*model implementations*: Implement a specific model (or model family) using a specific numerical method. An instance of a model implementation can be created by passing a model descriptor. Models can perform a calibration utilising the specific implementation. The calibrated model can be serialized back to a model descriptor.

*product implementations*: A product implementation implements the valuation of a financial product assuming a specific numerical method implemented by a model. An instance of a product implementation can be created by passing a product descriptor.

To allow the construction of products for a given model implementation without knowing the actual model and product implementation, models provide a product factory. That is all model implementations provide a method

```
ProductImplementation getProduct(ProductDescriptor productDescripor)
```

## Core Interfaces
The concept is mirrored via a few interfaces:

```
interface ProductDescriptor {
	// Marker Interface
}

interface ModelDescriptor {
	// Marker Interface
}

interface ProductImplementation {
	ProductImplementation of(ProductDescriptor productDescriptor);
	Valuation getValue(ModelImplementation model);
}

interface ModelImplementation {
	ModelImplementation of(ModelDescriptor modelDescriptor);
	ProductImplementation getProduct(ProductDescriptor productDescriptor);
}
```

## Calibration Roundtrip
The setup allows to perform calibrations using specific calibration products and numerical methods to be re-used in more general models. Consider for example an Equity Heston model implemented as Monte-Carlo simulation. While brute-force Monte-Carlo calibration is expensive, fast an accurate valuations (and hence calibrations) exists for a restricted set of products where fast Fourier transforms can be utilized. Hence, calibration of the Monte-Carlo Heston model can be performed in the following steps:

* Create a Fourier transform implementation of the Heston model.
* Create ProductDescriptors for the calibration products.
* The model will create Fourier transform representations for the calibration products.
* Generically calibrate the Heston model using Fourier transforms.
* Extract the model descriptor from the calibrated model.
* Create the Monte-Carlo Heston model from the model descriptor of the calibrated model.
* The Monte-Carlo model will create Monte-Carlo implementations of the calibration products using the Monte-Carlo Heston model’s product factory.
* Check that the Monte-Carlo Heston model is calibrated by checking the calibration products.
