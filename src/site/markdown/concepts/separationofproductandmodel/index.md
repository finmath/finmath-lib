# Separation of Product and Model and Numerical Method

02.02.2017, updated 22.06.2018.

[Work in progress. Will appear in a future version (4.x)]

## Introduction

The valuation library proposes the separation of financial products and models and numerical methods (as suggested in ISBN 0470047224).

For this, the library is structured in the following way:

- a hierachie of *model interfaces* related to asset classes/numerical methods with a 1:1 
relation to a hierarchy of *product interfaces* related to asset classes/numerical methods 
with a multitude of models and products implementing these interfaces.
- A 1:n relation of model descriptors to model implementations and a 1:n relation of 
product descriptors to product implementations, with factories providing a unified way to construct 
the specific implementation of a specific numerical method from the given model or product 
descriptors.

For details on the  hierachie of *model interfaces* and  *product interfaces* see the section
[model and product interfaces](modelandproductinterfaces.html).

For details on the  *model descriptors* and *product descriptors* see the section
[model and product descriptors](modelandproductdescriptors.html).
