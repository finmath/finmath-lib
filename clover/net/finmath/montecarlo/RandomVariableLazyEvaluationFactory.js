var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":31,"id":13120,"methods":[{"el":25,"sc":2,"sl":22},{"el":30,"sc":2,"sl":27}],"name":"RandomVariableLazyEvaluationFactory","sl":15}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_201":{"methods":[{"sl":27}],"name":"testAdd","pass":true,"statements":[{"sl":29}]},"test_209":{"methods":[{"sl":27}],"name":"testCap","pass":true,"statements":[{"sl":29}]},"test_299":{"methods":[{"sl":27}],"name":"testFloor","pass":true,"statements":[{"sl":29}]},"test_304":{"methods":[{"sl":27}],"name":"testGetQuantile","pass":true,"statements":[{"sl":29}]},"test_323":{"methods":[{"sl":27}],"name":"testRandomVariableStochastic","pass":true,"statements":[{"sl":29}]},"test_383":{"methods":[{"sl":27}],"name":"testRandomVariableStandardDeviation","pass":true,"statements":[{"sl":29}]},"test_462":{"methods":[{"sl":27}],"name":"testRandomVariableArithmeticSquaredPow","pass":true,"statements":[{"sl":29}]},"test_464":{"methods":[{"sl":22}],"name":"testRandomVariableDeterministc","pass":true,"statements":[{"sl":24}]},"test_534":{"methods":[{"sl":27}],"name":"testRandomVariableArithmeticSqrtPow","pass":true,"statements":[{"sl":29}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [464], [], [464], [], [], [201, 462, 209, 323, 534, 299, 304, 383], [], [201, 462, 209, 323, 534, 299, 304, 383], [], []]