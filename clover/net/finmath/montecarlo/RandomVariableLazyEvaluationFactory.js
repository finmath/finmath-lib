var clover = new Object();

// JSON: {classes : [{name, id, sl, el,  methods : [{sl, el}, ...]}, ...]}
clover.pageData = {"classes":[{"el":31,"id":13638,"methods":[{"el":25,"sc":2,"sl":22},{"el":30,"sc":2,"sl":27}],"name":"RandomVariableLazyEvaluationFactory","sl":15}]}

// JSON: {test_ID : {"methods": [ID1, ID2, ID3...], "name" : "testXXX() void"}, ...};
clover.testTargets = {"test_128":{"methods":[{"sl":27}],"name":"testCap","pass":true,"statements":[{"sl":29}]},"test_141":{"methods":[{"sl":27}],"name":"testAdd","pass":true,"statements":[{"sl":29}]},"test_206":{"methods":[{"sl":27}],"name":"testRandomVariableStandardDeviation","pass":true,"statements":[{"sl":29}]},"test_292":{"methods":[{"sl":27}],"name":"testRandomVariableArithmeticSqrtPow","pass":true,"statements":[{"sl":29}]},"test_333":{"methods":[{"sl":27}],"name":"testGetQuantile","pass":true,"statements":[{"sl":29}]},"test_367":{"methods":[{"sl":27}],"name":"testRandomVariableArithmeticSquaredPow","pass":true,"statements":[{"sl":29}]},"test_453":{"methods":[{"sl":22}],"name":"testRandomVariableDeterministc","pass":true,"statements":[{"sl":24}]},"test_496":{"methods":[{"sl":27}],"name":"testFloor","pass":true,"statements":[{"sl":29}]},"test_532":{"methods":[{"sl":27}],"name":"testRandomVariableStochastic","pass":true,"statements":[{"sl":29}]}}

// JSON: { lines : [{tests : [testid1, testid2, testid3, ...]}, ...]};
clover.srcFileLines = [[], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [], [453], [], [453], [], [], [333, 141, 496, 292, 128, 532, 367, 206], [], [333, 141, 496, 292, 128, 532, 367, 206], [], []]